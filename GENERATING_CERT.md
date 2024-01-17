# Keystore
Kmail uses a PKCS12 keystore to store certificates. Given a certificate and key (e.g, from CloudFlare), a keystore can be generated using the following command: 
```bash
openssl pkcs12 -export -inkey server.key -in server.pem -out server.p12
```
Configure the keystore to be used in [the configuration file](example/kmail.toml).

## Self-Signed (outdated)
To run a secure mail server, a certificate for the domain the mail server is handling is required. For this example, we wil generate a self-signed certificate.
```
openssl req -x509 -newkey rsa:4096 -keyout private.pem -out xgophers.crt -sha256 -days 365 -nodes
openssl pkcs8 -topk8 -inform PEM -outform DER -in private.pem -nocrypt -out private.der 
```

Then configure the certificate to be used in [the configuration file](example/kmail.toml).