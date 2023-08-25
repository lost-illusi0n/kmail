# Kmail
Kmail is a lightweight and straightforward mail server solution written in Kotlin. Most mail server solutions package together the same bundle of software, but with different configurations. Kmail instead implements all of these services from the ground up.

> **Warning**  
> Kmail is incomplete and under active development!

## A Quick Rundown
As important it is to make a mail server easy to host, through means of programming and scripts, knowledge of how a mail server operates is just as important. You have a message user agent (e.g., [Thunderbird](https://www.thunderbird.net/en-US/)) where you write and receive your emails. When you send an email, it is stored in a universal email format that any computer should be able to understand, and is sent over the internet to *your* message submission agent (MSA). The responsibility of an MSA is to begin the transfer of your email to its destination through the use of a message transfer agent (MTA). The MTA deals with any incoming or outgoing messages using the Simple Mail Transfer Protocol (SMTP). When the MTA receives an email, it will process and store it. The message user agent can use either the Internet Message Access Protocol (IMAP) or the Post Office Protocol 3 (POP3) to access any stored emails.

In short:\
MUA -(SMTP)-> MSA -> MTA -(SMTP)-> ... -(SMTP)-> MTA -> Mail Storage <-(IMAP/POP3)-> MUA

## MTA/MSA (SMTP)
The SMTP protocol is modeled in the [smtp](mailserver/smtp) module.\
The SMTP mail transfer agent is implemented in the [smtp-agent](mailserver/smtp-agent) module.

## Mail Storage (IMAP and POP3)
The IMAP protocol is modeled in the [imap](mailserver/imap) module.\
The POP3 protocol is modeled in the [pop3](mailserver/pop3) module.\
The IMAP agent is implemented in the [imap-agent](mailserver/imap-agent) module.\
The POP3 agent is implemented in the [pop3-agent](mailserver/pop3-agent) module.

## Users
WIP.

## Mail server
The mail server currently brings together all these components, configurable through the `kmail.toml` file, and runs a fully-fledged mail server. It is implemented in the [runner](mailserver/runner) module.