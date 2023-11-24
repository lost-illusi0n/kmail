# Kmail
Kmail is a lightweight mail server solution written in Kotlin. Instead of packing together the same bundle of software, Kmail instead implements all of these services from the ground up.

> **Warning**  
> Kmail is incomplete and under active development!

## Getting Started

### Docker
`docker pull ghcr.io/lost-illusi0n/kmail:master`
- map ports `143`, `110`, `587`, `25`
- add volumes that contain your `kmail.toml` and anything else used by Kmail (e.g., certificates)

### Prerequisites
For a smooth experience, you will need to:
- configure the [kmail.toml](kmail.toml) file
- create an accounts file (e.g. `accounts.kmail`), and set it in the kmail config.
    - each account is line seperated. account format is: `[email] [password-argon2-hash]`
- generate certificates (a self-signed example is documented [here](GENERATING_CERT.md))

## Development
Kmail is built and managed with Gradle. There is a root gradle project called `kmail-mailserver` in the [mailserver](mailserver) directory. All further instructions assume this is your working directory.

### Build
`./gradlew build` will build the mail server, including each module.

### API
Kmail exposes standalone libraries for [IMAP](mailserver/imap), [SMTP](mailserver/smtp), and [POP3](mailserver/pop3). They should be treated as standalone libraries and stay separate from Kmail internals. 

## Contributing
Contributions are welcome, however, due to the very early stage of development, please open an issue first discussing anything you would like to add or change.