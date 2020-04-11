# Technical Background

KirBot has been in active development for a few years now. During this time, the technology powering
the bot as well as the infrastructure it runs on has changed significantly. This document outlines
some of the infrastructure used to run the bot as well as security measures that have been put in
place to protect user's data.

## Core Services

KirBot is built in Kotlin using the [Java Discord API (JDA)](https://github.com/DV8FromTheWorld/JDA/)
library.

In addition to the bot itself, KirBot is supported by the following services:

* A Redis server for caching, configuration storage, and message passing between the panel and bot
* A nginx web server powering the admin panel written in PHP using the Laravel framework
* A MariaDB database for storing all configuration and logging data
* A prometheus server for statistical collection
* A Grafana dashboard for at-a-glance analytics and health monitoring.

## Security

KirBot is run in a docker-compose deployment on one of my servers. In addition to hosting KirBot,
this server powers some of my other projects which are containerized and isolated in Docker wherever
possible.

Data in KirBot's database is encrypted at rest via the use of MariaDB database encryption. KirBot
also uses a separate, dedicated, database to store its data which is not shared by any applications.

All the communication that KirBot does with the other supporting services happen internally over the
network automatically created by docker-compose when the stack is deployed. Everything non-essential
is not exposed to the internet in any way.

## Source

KirBot's source code is available in a public GitHub repository visible [here](https://github.com/mrkirby153/KirBot).  
The web admin panel's source code is available in a public GitHub repository visible [here](https://github.com/mrkirby153/KirBotPanel).

All commits to both the bot and panel's repository are signed using PGP to ensure integrity.