#![feature(str_strip)]
extern crate jni;
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jint, jstring};
use jni::JNIEnv;
use std::{fs::File, os::unix::io::FromRawFd};
mod dns_actions;
extern crate libc;
extern crate state_list;
use state_list::StateList;
use std::io::prelude::*;
extern crate android_logger;
use android_logger::Config;
use log::Level;
#[macro_use]
extern crate log;
use state::Storage;
use std::sync::RwLock;
mod packeemon;
use packeemon::{UDPPacket, IPPacket};
mod pollinator;
mod domain_filter;


static TRAFFIC_WAIT_WRITE: StateList<Vec<u8>> = StateList::new();
static TRAFFIC_IN: StateList<Vec<u8>> = StateList::new();
static JAVA_CALLBACKS: StateList<FilterCallback> = StateList::new();

static JVM: Storage<jni::JavaVM> = Storage::new();
static ALLOW_LIST: StateList<String> = StateList::new();
static TMP_LIST: StateList<String> = StateList::new();
static STROLCH_SETTINGS: Storage<StrolchSettings> = Storage::new();
static TUNNEL_STOP: Storage<RwLock<bool>> = Storage::new();

static HANDLE_ANDROID_CONNECT_CHECK: bool = false;

pub struct StrolchSettings {
    doh_server_name: String,
    doh_server_ip: String,
    doh_server_query: String,
}

#[derive(std::clone::Clone, std::cmp::PartialEq)]
pub struct FilterCallback {
    domain: String, 
    filter: String, 
    state: domain_filter::FResp,
}

#[no_mangle]
pub extern "C" fn Java_de_batram_dnsstrolch_TunnelVision_00024Companion_innit(
    env: JNIEnv,
    _class: JClass,
    jrules: JString,
    jdoh_server_name: JString,
    jdoh_server_ip: JString,
    jdoh_server_query: JString,
) {
    android_logger::init_once(Config::default().with_min_level(Level::Trace));    
    
    TRAFFIC_WAIT_WRITE.init_empty();
    TRAFFIC_IN.init_empty();
    JAVA_CALLBACKS.init_empty();

    let rules: String = env.get_string(jrules).expect("").into();
    debug!("got rules: {}", rules);
    ALLOW_LIST.init_string(rules);
    //extract hardcoded ips from allow list
    for entry in ALLOW_LIST.get_entries() {
        if entry.starts_with("=") {
            domain_filter::add_hardcoded_ip(entry.as_str());
        }
    }

    TMP_LIST.init_empty();
    TUNNEL_STOP.set(RwLock::new(false));

    //let callback = env.new_global_ref(callback).unwrap();
    let doh_server_name: String = env.get_string(jdoh_server_name).expect("").into();
    let doh_server_ip: String = env.get_string(jdoh_server_ip).expect("").into();
    let doh_server_query: String = env.get_string(jdoh_server_query).expect("").into();

    STROLCH_SETTINGS.set(
        StrolchSettings {
            doh_server_name,
            doh_server_ip,
            doh_server_query,        
        }
    );

    JVM.set(env.get_java_vm().unwrap());
}

#[no_mangle]
pub extern "C" fn Java_de_batram_dnsstrolch_TunnelVision_00024Companion_update_1settings(
    env: JNIEnv,
    _class: JClass,
    jdoh_server_name: JString,
    jdoh_server_ip: JString,
    jdoh_server_query: JString,
) {
    let doh_server_name: String = env.get_string(jdoh_server_name).expect("").into();
    let doh_server_ip: String = env.get_string(jdoh_server_ip).expect("").into();
    let doh_server_query: String = env.get_string(jdoh_server_query).expect("").into();

    STROLCH_SETTINGS.set(
        StrolchSettings {
            doh_server_name,
            doh_server_ip,
            doh_server_query,        
        }
    );
}

#[no_mangle]
pub extern "C" fn Java_de_batram_dnsstrolch_TunnelVision_00024Companion_hash_1domain(
    env: JNIEnv,
    _class: JClass,
    jdomain: JString,
) -> jstring {
    let domain: String = env.get_string(jdomain).expect("").into();
    let hash = domain_filter::hash_domain(domain.as_str());

    let output = env.new_string(hash).expect("JString fail!");

    return output.into_inner();
}

#[no_mangle]
pub extern "C" fn Java_de_batram_dnsstrolch_TunnelVision_00024Companion_add_1filter(
    env: JNIEnv,
    _class: JClass,
    jrule: JString,
    jtype: domain_filter::FResp,
) {
    let new_rule: String = env.get_string(jrule).expect("").into();

    match jtype {
        domain_filter::FResp::Hardcoded => {
            debug!("hardcoded rule: {}", new_rule);
            domain_filter::add_hardcoded_ip(new_rule.as_str());
        },
        _ => {
            debug!("got new rule: {} {:?}", new_rule, jtype);
        
            ALLOW_LIST.add_item(new_rule);    
        }
    }
}

#[no_mangle]
pub extern "C" fn Java_de_batram_dnsstrolch_TunnelVision_00024Companion_remove_1filter(
    env: JNIEnv,
    _class: JClass,
    jrule: JString,
    jtype: domain_filter::FResp,
) {
    let new_rule: String = env.get_string(jrule).expect("").into();

    match jtype {
        domain_filter::FResp::Hardcoded => {
            debug!("hardcoded rule: {}", new_rule);
            domain_filter::remove_hardcoded_ip(new_rule.as_str());
        },
        _ => {
            debug!("trying to remove rule: {}", new_rule);

            ALLOW_LIST.remove(&new_rule);
        }
    }

}

#[no_mangle]
pub extern "C" fn Java_de_batram_dnsstrolch_TunnelVision_00024Companion_close_1tunnel(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut stop = TUNNEL_STOP.get().write().unwrap();
    *stop = true;
}

pub fn block_callback(fc: FilterCallback, env: &JNIEnv){
    let domain_jst = env.new_string(fc.domain).unwrap();
    let filter_jst = env.new_string(fc.filter).unwrap();

    match env.call_static_method("de/batram/dnsstrolch/TunnelVision", "block_callback", "(Ljava/lang/String;Ljava/lang/String;I)V",
            &[JObject::from(domain_jst).into(), JObject::from(filter_jst).into(), JValue::Int(fc.state as i32)]
        ) {
        Ok(e) =>   debug!("call success lib: {:?}", e),
        Err(e) =>  debug!("call failed lib: {:?}", e),
    }        
}

#[no_mangle]
pub extern "C" fn Java_de_batram_dnsstrolch_TunnelVision_00024Companion_handoff_1tunnel(
    env: JNIEnv,
    _class: JClass,
    fd: jint,
) -> jint {

    let mut filio = unsafe { File::from_raw_fd(fd) };

    loop {
        let mut stop = TUNNEL_STOP.get().write().unwrap();
        if *stop {
            //Close tunnel
            //TODO: Maybe try to empty out write list?
            *stop = false;
            debug!("closing tunnel");
            return 1;
        }
    
        if TRAFFIC_WAIT_WRITE.length() > 0 {
            pollinator::write_read(fd, &mut filio, write_packets, read_packets)
        } else {
            pollinator::read(fd, &mut filio, read_packets)
        }

        if JAVA_CALLBACKS.length() > 0 {
            if let Some(callback) = JAVA_CALLBACKS.pop() {
                block_callback(callback, &env);
            }       
        } 
    }
}

fn read_packets(filio: &mut File){
    let mut buf = [0 as u8; 1000];
    match filio.read(&mut buf) {
        Ok(size) => Some(buf[..size].to_vec()),
        Err(e) => {debug!("pollread file err: {:?}", e); None}
    }.map(
        |packet| {
            TRAFFIC_IN.add_item(packet);
            std::thread::spawn( move || {
                if TRAFFIC_IN.length() > 0 {
                    if let Some(packet) = TRAFFIC_IN.pop() {
                        packeemon::might_be_dns(packet).map( |pkt_tuple| {
                            handle_dns_packet(pkt_tuple);
                        });
                    }       
                } else {
                    debug!("what????");
                }
            });
        }
    );
}

fn write_packets(filio: &mut File){
    //TODO: do we try to write again on error?
    if let Some(mut data) = TRAFFIC_WAIT_WRITE.pop() {
        match filio.write(&mut data) {
            Err(e) => debug!("pollwrite file err: {:?}", e),
            Ok(_i) => {},
        }
    }
}

fn handle_dns_packet(tuple: (IPPacket, UDPPacket)) {
    let (ip_pkt, udp_pkt) = tuple;

    if let Ok(dns_pkt) = dns_parser::Packet::parse(udp_pkt.data.as_slice()) {

        let domain = domain_filter::find_domain_name(&dns_pkt).unwrap();
        let qtype = dns_pkt.questions[0].qtype;

        //domain = domain_filter::remove_wait_cname(domain);
        if let Some(hard_ip) = domain_filter::get_hardcoded_ip(&domain, qtype) {
            let dns_answer = dns_actions::local_answer(&dns_pkt, hard_ip, &domain, i32::max_value() >> 2);
            enqueue_dns_response(&dns_answer, ip_pkt, udp_pkt.src_port);
            JAVA_CALLBACKS.add_item(FilterCallback{
                domain: domain.clone(),
                filter: format!("{} {}", domain, hard_ip), 
                state: domain_filter::FResp::Hardcoded,
            });
            return;
        }  else {
            if HANDLE_ANDROID_CONNECT_CHECK {
                if let Some(hard_ip) = domain_filter::must_be_google_test(&domain, qtype) {
                    let dns_answer = dns_actions::none_answer_answer(&dns_pkt, hard_ip, &domain);
                    enqueue_dns_response(&dns_answer, ip_pkt, udp_pkt.src_port);
                    JAVA_CALLBACKS.add_item(FilterCallback{
                        domain: domain.clone(),
                        filter: domain.clone(),
                        state: domain_filter::FResp::Unknown,
                    });
                    return;
                }
            }

            let (reason, allowed, filter) = domain_filter::allow_request(&domain, &ALLOW_LIST, &TMP_LIST);
            if allowed {
                std::thread::spawn(move || {
                    let settings = STROLCH_SETTINGS.get();

                    let dns_answer = dns_actions::doh_lookup(&settings.doh_server_name.as_str(), &settings.doh_server_ip.as_str(), &settings.doh_server_query.as_str(), &udp_pkt.data.as_slice());
                    enqueue_dns_response(&dns_answer, ip_pkt, udp_pkt.src_port);
                }); 
            } else {
                match reason {
                    domain_filter::FResp::Ignored => {
                        let dns_answer = dns_actions::none_answer_answer(&dns_pkt, "0.0.0.0".parse().unwrap(), &domain);
                        enqueue_dns_response(&dns_answer, ip_pkt, udp_pkt.src_port);    
                    }
        
                    _ => {},
                }    
            }

            JAVA_CALLBACKS.add_item(FilterCallback{
                domain: domain.clone(), 
                filter, 
                state: reason,
            });

        } 
    }
}

fn enqueue_dns_response(dns_reponse: &Vec<u8>, og_pkt: IPPacket, port: u16){
    let mut udpp = UDPPacket::build(dns_reponse, 53, port);
    let mut resp_ip_pkt = og_pkt.clone();

    resp_ip_pkt.switch_dst_src();
    let ndata = resp_ip_pkt.place_new_data(&mut udpp);

    TRAFFIC_WAIT_WRITE.add_item(ndata);
}  