# HomeManager
> A Minecraft plugin to manage player homes with location saving and teleportation.

HomeManager is a Minecraft plugin that allows players to save, delete, list, and teleport to their homes in-game. It uses an SQLite database to store home locations and makes it easy for players to manage their homes.

## Installing / Getting started

To get started with HomeManager, you'll need to install it on your server. If you're using a plugin manager, follow these steps:

1. Download the HomeManager plugin JAR file.
2. Place the JAR file in your server's `plugins` folder.
3. Restart the server to load the plugin.

Once installed, players can use the `/home` command to create, list, teleport to, and delete their homes.

### Initial Configuration

HomeManager does not have any configuration but I will eventually plan on implementing it as time goes on.

### Building

After cloning the repository, you can build the project by running:

```shell
./gradlew build
```

This will generate the plugin JAR file located in the `target/` directory.

### Deploying / Publishing

To deploy the plugin to your Minecraft server, simply copy the generated JAR file from `build/libs/HomeManager-YYMMDD-HHMM-SNAPSHOT.jar` into the `plugins/` directory of your server, and restart the server.

## Features

HomeManager provides several features for managing player homes:
* **Create homes**: Players can create homes at their current location using `/home create <home name>`.
* **List homes**: Players can list all their saved homes using `/home list`.
* **Delete homes**: Homes can be deleted using `/home delete <home name>`.
* **Teleport to homes**: Players can teleport to their saved homes using `/home teleport <home name>`.
* **Update home locations**: Players can update the location of an existing home with `/home set <home name>`.

## Configuration

Usage (we'll take *MyHome* for example):

- Create a home at your current location:
	```
	/home create MyHome 
	```

- List of all homes you have created.
	```
	/home list
	```

- Delete a home
	```bash
	/home delete MyHome
	```

- Teleport to a home 
	```bash
	/home teleport MyHome
	```

- Updates a house to be at your current location.
	```bash
	/home set MyHome
	```

- If you forget how to use this plugin,
	```bash
	/home help
	```

### `config.yml`

This plugin uses a YAML file for configuration. Here's the general structure:

|Value|Default|Description|
|---|---|---|
|`postgres.enabled`|`false`|Whether the server should use PostgreSQL or SQLite|
|`postgres.host`|`localhost`|Database's host, I don't think you should change this value|
|`postgres.port`|5432|The port to access PostgreSQL. Should be 5432 unless you're doing some weird things on your side|
|`postgres.database`|`home_manager`|The table where homes will be saved in|
|`postgres.username`|`postgres`|The username which owns the DB table. Change this for security purposes|
|`postgres.password`|None|If you have PostgreSQL enabled and don't have a password, the plugin will throw an error|

In case you don't wanna set a password, you can run the server with `-Dhomemanager.ignore-empty-password=true` flag.

## Contributing

We welcome contributions to HomeManager! If you'd like to contribute:

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature-branch`).
3. Commit your changes (`git commit -am 'Add new feature'`).
4. Push to the branch (`git push origin feature-branch`).
5. Submit a pull request.

Feel free to open an issue if you encounter any bugs or have feature requests.

## Links

Here are some useful links to the HomeManager project:

- Project homepage: <https://github.com/walker84837/HomeManager>
- Repository: <https://github.com/walker84837/HomeManager>
- Issue tracker: <https://github.com/walker84837/HomeManager/issues>

## Licensing

The code in this project is licensed under the [MPL-2.0](LICENSE).
