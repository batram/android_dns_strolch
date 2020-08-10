use dns_parser::Packet;
use state::Storage;
use state_list::StateList;
use std::collections::HashMap;
use std::net::IpAddr;
use std::sync::Mutex;
use std::{thread, time};
use tld;
use seahash;

pub static HARDMAPPED_DOMAINS: Storage<Mutex<HashMap<String, IpAddr>>> = Storage::new();

#[derive(Debug, std::clone::Clone, std::cmp::PartialEq)]
pub enum FResp {
    Allowed,
    StarAllowed,
    Hardcoded,
    Unknown,
    TMPList,
    Ignored,
    Hashed
}

pub fn allowlist_matches(domain: &str, allow_list: &StateList<String>) -> (FResp, bool, String) {
    let mut prelim_return = (FResp::Unknown, false, domain.to_string());
    let mut hashed_domain = String::new();

    for entry in allow_list.get_entries() {
        if entry.starts_with("*.") {
            let matchme = entry.strip_prefix("*").unwrap();

            if domain.ends_with(matchme) {
                return (FResp::StarAllowed, true, domain.to_string());
            }
        } else if entry.starts_with("#") {   
            if hashed_domain.is_empty() {
                hashed_domain = hash_domain(domain);
            }          
            if hashed_domain == entry {
                return (FResp::Hashed, true, entry);
            }
        } else if entry.starts_with("!") {
            let mut mod_entry = entry.strip_prefix("!").unwrap();
            if mod_entry.starts_with("*.") {
                mod_entry = mod_entry.strip_prefix("*").unwrap();
            }

            if domain.ends_with(mod_entry) {
                prelim_return = (FResp::Ignored, false, entry);
            }
        } else if entry == domain {
            return (FResp::Allowed, true, domain.to_string());
        }
    }
    return prelim_return;

}

pub fn find_domain_name(pkt: &Packet) -> Option<String> {
    if pkt.questions.len() >= 1 {
        let firstq = pkt.questions.first().unwrap();
        return Some(format!("{}", firstq.qname));
    }

    if pkt.answers.len() >= 1 {
        let firsta = pkt.answers.first().unwrap();
        return Some(format!("{}", firsta.name));
    }
    return None;
}

pub fn parse_config_line(line: &str) -> Option<(String, IpAddr)>{
    let mut trimmed = line.trim();
    debug!("parse to parse: {:?}", line);

    if !trimmed.is_empty() {
        if trimmed.starts_with("=") {
            trimmed = trimmed.strip_prefix("=").unwrap();
        }
        let split: Vec<&str> = trimmed.split_ascii_whitespace().collect();
        if split.len() == 2 {
            split[1].parse()
                .map(|ip: IpAddr| Some((split[0].to_string(), ip)))
                .unwrap_or_else( |e| {
                    debug!("wuups, failed to parse ip: {:?} {:?}", split, e);
                    None
                })
        } else {
            debug!("wuups, don't understand config: {:?}", split);
            None
        }
    } else {
        None
    }
}

pub fn parse_host_list(str: &str) -> HashMap<String, IpAddr> {
    let mut mapped_ips = HashMap::<String, IpAddr>::new();

    for line in str.split("\n") {
        parse_config_line(line).map({
            |(domain, ip)| mapped_ips.insert(domain, ip)
        });
    }

    return mapped_ips;
}

pub fn get_hard_mapped_hosts() -> HashMap<String, IpAddr> {
    let entries = "";
    return parse_host_list(entries);
}

pub fn get_hardcoded_ip(name: &String, qtype: dns_parser::QueryType) -> Option<IpAddr> {
    let mapped_ips = HARDMAPPED_DOMAINS
        .get_or_set(|| Mutex::new(get_hard_mapped_hosts()))
        .lock()
        .unwrap();

    let ipv6_option = "ipv6::".to_string() + name.as_str();

    let key = match qtype {
        dns_parser::QueryType::AAAA => &ipv6_option,
        dns_parser::QueryType::A => name,
        _ => return None,
    };

    return mapped_ips.get(key).map(|ip| ip.clone());
}

pub fn must_be_google_test(name: &String, qtype: dns_parser::QueryType) -> Option<IpAddr> {
    debug!("google_test: {:?}, {:?} ", name, qtype);

    if name.contains(".") {
        return None;
    }

    return match qtype {
        dns_parser::QueryType::AAAA => Some("::1".parse().unwrap()),
        dns_parser::QueryType::A => Some("0.0.0.0".parse().unwrap()),
        _ => None,
    };
}

pub fn add_hardcoded_ip(str: &str){
    parse_config_line(str).map({
        |(domain, ip)| 
        //TODO: ipv6???
        HARDMAPPED_DOMAINS
        .get_or_set(|| Mutex::new(get_hard_mapped_hosts())).lock().unwrap().insert(domain, ip)
    });
}

pub fn remove_hardcoded_ip(str: &str){
    parse_config_line(str).map({
        |(domain, _ip)| 
        //TODO: ipv6???
        HARDMAPPED_DOMAINS
        .get_or_set(|| Mutex::new(get_hard_mapped_hosts())).lock().unwrap().remove(&domain)
    });
}

pub fn remove_wait_cname(mut domain: String) -> String {
    if domain.ends_with(".plzwait") {
        thread::sleep(time::Duration::from_millis(1000));
        domain = domain.strip_suffix(".plzwait").unwrap().to_string();
    }

    return domain;
}

pub fn allow_request(
    domain: &String,
    allow_list: &StateList<String>,
    tmp_list: &StateList<String>,
) -> (FResp, bool, String) {
    //TODO: build b-tree or something fancy :>D
    let (resp, matches, filter) =  allowlist_matches(&domain.as_str(), allow_list);

    if matches {
        return (resp, true, filter);
    }

    if tmp_list.contains(&domain) {
        return (FResp::TMPList, true, domain.to_string());
    }

    return (resp, false, filter);
}

pub fn hash_domain(domain: &str) -> String {
    return format!("#{:16X}", seahash::hash(domain.as_bytes()));
}

pub fn dot_reverse(str: &String) -> String {
    let mut split = str.split('.').collect::<Vec<&str>>();
    split.reverse();
    return split.join(".");
}

pub fn top_level_filter(domain: &str) -> Result<String, String> {
    let split = domain.split('.').collect::<Vec<&str>>();
    let vlen = split.len();
    if vlen >= 3 {
        let top2 = split[vlen - 2].to_string() + "." + split[vlen - 1];
        if tld::exist(top2.as_str()) {
            let mdomain = "*.".to_string() + split[vlen - 3] + "." + top2.as_str();
            return Ok(mdomain.to_string());
        }
    }
    if vlen >= 2 {
        let top1 = split.last().unwrap();
        if tld::exist(top1) {
            let mdomain = "*.".to_string() + split[vlen - 2] + "." + top1;
            return Ok(mdomain.to_string());
        }
    }

    return Err(format!("Can't determine top level domain of {}", domain));
}