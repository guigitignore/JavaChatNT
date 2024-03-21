# JavaChatNT

JavaChatNT implements both heavy client and server compliant with the DuckyChat protocol.

## Requirements

### Linux

Packages to install:

- JDK (>=8)
- make

If you want to use heavy client with a smartcard, you need to install:

- On debian distros: `libpcsclite-dev` and `pcscd` packages
- On arch based distros: `pcsclite` and `ccid` packages

Then you can start `pcscd.service`
To test your smartcard reader, it is recommanded to install `pcsc-tools` package and to run `pcsc_scan` command

You can find more informations on these websites:
[https://jpmrblood.github.io/linux/driver/tips/install-ocf-pcsc-omnikey5432/]()
[https://muscle.apdu.fr/musclecard.com/middle.html]()

### Windows

- JDK (>=8)
- make
- netcat (if you want to use telnet client)

You can use `chocolatey` package manager to install these packages: [https://docs.chocolatey.org/en-us/choco/setup]()

If you want to use heavy client with a smartcard you will need to install jdk 8 x86 version (you need to register on oracle website to download installer):
[https://www.oracle.com/webapps/redirect/signon?nexturl=https://download.oracle.com/otn/java/jdk/8u202-b08/1961070e4c9b4e26a04e7f5a083f551e/jdk-8u202-windows-i586.exe]()

## Building

`make` will compile all java classes

If you want to test server: `make server` (it will listen on port 2000 for USER accounts and 2001 for ADMIN accounts)
If you want to test heavy client: `make client` (it will connect to the server using port 2000)
If you want to test heavy cleint with a smartcard: `make card-client`

To build jar file and release the project: `make release`. 
It will create a folder named `out` and put all necessary files in it. 
It will create a launcher named `start.sh` on Linux and `start.bat` on windows.
You can copy this folder in another location.

## Running

Go to the release folder and open a terminal in this location:
Syntax expected: `./start.sh <mode> <mode args>`
There is currently 4 available modes:

### Server mode

Syntax: `./start.sh server <PORT> [<PORT2>] ...`
With this basic syntax, you can make the server listening on several port at the same time.
You can specify which type of user is allowed using `:` separator.
Currently there is only two types of account: `USER` and `ADMIN`

`./start server 2000:USER 2001:USER,ADMIN`
/!\ the order is very important. If an account does not exist, it will be automatically registered using the first account type specified. 
You should always put the lowest privilege account type in first place if you open this port to others.

### Telnet client

You can use `netcat` (recommanded) or `telnet` to join the server. 
You will not be able to send files, cipher data or to use public key authentification.

### Heavy client

Syntax: `./start.sh connect <PORT>` or `./start.sh connect <HOST>:<PORT>`
Using this client, you will be able to send files, cipher data and use public key authentification.

### Heavy client with smartcard

It uses the same syntax than heavy client: juste use `card-connect` mode instead.
If username is not already registered, it will create a user and login automatically using public key authentification

### Generator

If you want to use public key authentification with heacy client, you must generate a RSA 1024 key pair.
Syntax: `./start.sh generate <USERNAME>`
It will create a `keys` folder and create two files named `<USERNAME>.pub` and `<USERNAME>.priv`
It you register a new user with a keypair defined in this folder, it will automatically use public key authentification

## Commands

## Logs

Logs are stored inside `logs` folder. A new log file is created each time you are running a new instance of a server or a heavy client.
They are automatically named with the date and time of the process creation.

## About Project
