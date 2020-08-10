use libc::{c_int, c_short, c_uint};
use std::{fs::File};

#[repr(C)]
pub struct pollfd {
    fd: c_int,
    events: c_short,
    revents: c_short,
}
pub const POLLIN: c_short = 1;
pub const POLLOUT: c_short = 2;
pub const POLLWRBAND: c_short = 256;

extern "C" {
    pub fn poll(fds: *mut pollfd, nfds: c_uint, timeout: c_int) -> c_int;
}

pub fn write_read(fd: i32, filio: &mut File, write: fn(&mut File), read: fn(&mut File)) {
    let mut fdset = pollfd {
        fd: fd,
        events: POLLIN | POLLOUT | POLLWRBAND,
        revents: 0,
    };
    
    match unsafe { poll(&mut fdset as *mut _, 1, 0) } {
        ret if ret < 0 => {
            panic!("pollread error: {} \n", std::io::Error::last_os_error());
        }
        ret if ret > 0 && (fdset.events & POLLOUT) != 0 => {
            write(filio);
        }
        ret if ret > 0 && (fdset.events & POLLWRBAND) != 0 => {
            write(filio);
        }
        ret if ret > 0 && ((fdset.events & POLLIN) != 0) => {
            read(filio);
        }
        _ => {}
    }
}

pub fn read(fd: i32, filio: &mut File, read: fn(&mut File)) {
    let mut fdset = pollfd {
        fd: fd,
        events: POLLIN,
        revents: 0,
    };
    
    match unsafe { poll(&mut fdset as *mut _, 1, 3) } {
        ret if ret < 0 => {
            panic!("pollread error: {} \n", std::io::Error::last_os_error());
        }
        ret if ret > 0 && ((fdset.events & POLLIN) != 0) => {
            read(filio);
        }
        _ => {}
    }
}

