CARGO_NDK := $(shell cargo ndk --version)

aarch64-linux-android : SODIR := ../main/jniLibs/arm64-v8a/
aarch64-linux-android: cargo-ndk
	cargo ndk --platform 21 --target aarch64-linux-android build --release
	[ -d $(SODIR) ] || mkdir -p $(SODIR);
	cp target/aarch64-linux-android/release/libtunnelvision.so $(SODIR)

cargo-ndk: 
ifndef CARGO_NDK
	cargo install cargo-ndk
endif

armv7-linux-androideabi : SODIR := ../main/jniLibs/armeabi-v7a/
armv7-linux-androideabi: cargo-ndk
	cargo ndk --platform 21 --target armv7-linux-androideabi build --release
	[ -d $(SODIR) ] || mkdir -p $(SODIR);
	cp target/armv7-linux-androideabi/release/libtunnelvision.so  $(SODIR)

#rustup target install x86_64-linux-android
x86_64-linux-android : SODIR := ../main/jniLibs/x86-64/
x86_64-linux-android: cargo-ndk
	cargo ndk --platform 21 --target x86_64-linux-android build --release
	[ -d $(SODIR) ] || mkdir -p $(SODIR)
	cp target/x86_64-linux-android/release/libtunnelvision.so $(SODIR)

#rustup target install i686-linux-android
i686-linux-android : SODIR := ../main/jniLibs/x86/
i686-linux-android: cargo-ndk
	cargo ndk --platform 21 --target i686-linux-android build --release
	[ -d $(SODIR) ] || mkdir -p $(SODIR)
	cp target/i686-linux-android/release/libtunnelvision.so ../main/jniLibs/x86/

all: aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android

clean:
	cargo clean
