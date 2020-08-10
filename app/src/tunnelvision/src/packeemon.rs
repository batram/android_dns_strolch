use std::net::Ipv4Addr;

#[derive(Debug, Hash, Eq, PartialEq, Clone)]
pub struct IPHeader {
    pub version: u8,
    pub ihl: u8,
    pub tos: u8,
    pub total_len: u16,
    pub ident: u16,
    pub flags_fgoff: u16,
    pub ttl: u8,
    pub proto: u8,
    pub checksum: u16,
    pub source_address: std::net::Ipv4Addr,
    pub dest_address: std::net::Ipv4Addr,
}

#[derive(Debug, Hash, Eq, PartialEq, Clone)]
pub struct IPPacket {
    pub header: IPHeader,
    pub ogbuf: Vec<u8>,
}

impl IPPacket {
    pub fn parse(buf: Vec<u8>) -> IPPacket {
        let pkt = IPPacket {
            header: IPHeader {
                version: (0b11110000 & buf[0]) >> 4,
                ihl: (0b00001111 & buf[0]),
                tos: buf[1],
                total_len: ((buf[2] as u16) << 8) + buf[3] as u16,
                ident: ((buf[4] as u16) << 8) + buf[5] as u16,
                flags_fgoff: ((buf[6] as u16) << 8) + buf[7] as u16,
                ttl: buf[8],
                proto: buf[9],
                checksum: ((buf[10] as u16) << 8) + buf[11] as u16,
                source_address: Ipv4Addr::new(buf[12], buf[13], buf[14], buf[15]),
                dest_address: Ipv4Addr::new(buf[16], buf[17], buf[18], buf[19]),
            },
            ogbuf: buf,
        };

        return pkt;
    }

    pub fn checksum(&self) -> u16 {
        let hlen = (self.header.ihl * 4) as usize;
        let mut offset = 0 as usize;
        let mut checksum = 0xffffu32 as u32;
        while offset + 1 < hlen {
            if offset != 10 {
                checksum += ((self.ogbuf[offset] as u32) << 8) + (self.ogbuf[offset + 1] as u32)
            }
            offset += 2;

            if checksum > 0xffff {
                checksum -= 0xffff;
            }
        }

        !checksum as u16
    }

    pub fn switch_dst_src(&mut self) {
        self.ogbuf.swap(12, 16);
        self.ogbuf.swap(13, 17);
        self.ogbuf.swap(14, 18);
        self.ogbuf.swap(15, 19);

        let checky = self.checksum().to_be_bytes();
        self.ogbuf[10] = checky[0];
        self.ogbuf[11] = checky[1];
    }

    pub fn place_new_data(&mut self, data: &mut Vec<u8>) -> Vec<u8> {
        //fixup total_len
        let nlenb = (((self.header.ihl * 4) as u16 + data.len() as u16) as u16).to_be_bytes();
        self.ogbuf[2] = nlenb[0];
        self.ogbuf[3] = nlenb[1];
        self.ogbuf.truncate((self.header.ihl * 4) as usize);
        self.ogbuf.append(data);

        let checky = self.checksum().to_be_bytes();
        self.ogbuf[10] = checky[0];
        self.ogbuf[11] = checky[1];

        return self.ogbuf.clone();
    }

    pub fn get_payload(&self) -> Vec<u8> {
        let offset = (self.header.ihl * 4) as usize;
        return (&self.ogbuf[offset..]).to_vec();
    }
}


#[derive(Debug, Hash, Eq, PartialEq)]
pub struct UDPPacket {
    pub src_port: u16,
    pub dest_port: u16,
    pub length: u16,
    pub check: u16,
    pub data: Vec<u8>,
}

impl UDPPacket {
    pub fn build(data: &Vec<u8>, src_port: u16, dest_port: u16) -> Vec<u8> {
        let length = (data.len() + 8) as u16;
        let mut rvec = Vec::<u8>::new();

        let mut spb = src_port.to_be_bytes().to_vec();
        rvec.append(&mut spb);
        let mut dpb = dest_port.to_be_bytes().to_vec();
        rvec.append(&mut dpb);

        let mut lenb = length.to_be_bytes().to_vec();
        rvec.append(&mut lenb);

        //don't care about checksums :D
        rvec.append(&mut vec![0, 0]);
        rvec.append(&mut data.clone());

        return rvec;
    }

    pub fn parse(buf: Vec<u8>) -> UDPPacket {
        UDPPacket {
            src_port: ((buf[0] as u16) << 8) + buf[1] as u16,
            dest_port: ((buf[2] as u16) << 8) + buf[3] as u16,
            length: ((buf[4] as u16) << 8) + buf[5] as u16,
            check: ((buf[6] as u16) << 8) + buf[7] as u16,
            data: buf[8..].to_vec(),
        }
    }
}

pub fn might_be_dns(pkt_vec: Vec::<u8>) -> Option<(IPPacket, UDPPacket)> {
    let ippkt = IPPacket::parse(pkt_vec);
    if ippkt.header.version == 4 && ippkt.header.proto == 17 {
        //UDP
        let udpppp = UDPPacket::parse(ippkt.get_payload());
        if udpppp.length > 8
            && udpppp.dest_port == 53 {
                return Some((ippkt, udpppp))
        }
    }
    None
}