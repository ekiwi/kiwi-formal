#!/usr/bin/env bash
# https://github.com/upscale-project/pono

VERSION=0.1.1

# clean
rm -f v${VERSION}.zip
rm -rf pono-*


# install ubuntu packets
if [ `lsb_release -si` == "Ubuntu" ]; then
sudo apt-get install -y build-essential bison flex python3-dev libgmp-dev libantlr3c-dev
fi


# download
wget https://github.com/upscale-project/pono/archive/v${VERSION}.zip
unzip v${VERSION}.zip

# compile
cd pono-${VERSION}
./contrib/setup-smt-switch.sh
./contrib/setup-btor2tools.sh
./configure.sh
cd build
make -j`nproc`


# test
#./build/pono -e bmc -k 1 ../test.btor

# copy
cd ..
mkdir -p bin-pono
mv pono-${VERSION}/build/pono bin-pono/
mv pono-${VERSION}/build/libpono.so bin-pono/
