# Generating a certificate
To run a secure mail server, a certificate for the domain the mail server is handling is required. For this example, we wil generate a self-signed certificate.
```
openssl req -x509 -newkey rsa:4096 -keyout private.pem -out xgophers.crt -sha256 -days 365 -nodes
openssl pkcs8 -topk8 -inform PEM -outform DER -in private.pem -nocrypt -out private.der 
```

Then configure the certificate to be used in [the configuration file](kmail.toml).