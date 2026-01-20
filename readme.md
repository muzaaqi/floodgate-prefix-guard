# FloodgatePrefixGuard 🛡️

**FloodgatePrefixGuard (FPG)** is a security and utility plugin designed for Minecraft servers running **Geyser** and **Floodgate**. It prevents Bedrock players from joining the server if their username prefix (e.g., `.`) fails to load.

This plugin effectively eliminates the common **"UUID Desync"** issue, where Bedrock players accidentally join as "Offline Java" players, resulting in inventory loss, rank issues, and data corruption.

## 🛑 The Problem

Sometimes, due to network lag, `key.pem` desynchronization, or server configuration errors, Floodgate fails to inject the prefix into a Bedrock player's username during the login handshake.

When this happens:

1. The server treats the Bedrock player as a **Java Offline Player**.
2. The server generates a **different UUID** (Java Offline UUID) instead of the correct Floodgate UUID.
3. **Result:** The player logs in with an empty inventory, no permissions, and creates a "ghost" player file.

## ✅ The Solution

**FloodgatePrefixGuard** sits at the `PlayerLoginEvent` (High Priority) and performs a multi-layer check:

1. **Floodgate API Check:** Verifies if the incoming session is a valid Floodgate player.
2. **Geyser API Fallback:** If Floodgate fails, it cross-references the **Geyser API** to detect if the UUID or Username belongs to a connected Bedrock client.
3. **Prefix Validation:** If a player is confirmed as Bedrock but is missing the required prefix (e.g., `.`), they are immediately kicked with a helpful message.

## ✨ Features

* **Multi-Layer Detection:** Uses both Floodgate API and Geyser API (UUID + Name Matching) to catch 100% of Bedrock players, even when Floodgate bugs out.
* **Linked Account Bypass:** Option to allow Bedrock players linked to Java accounts (Global Linking) to join without a prefix.
* **Staff Notifications:** Alerts online staff when a player is blocked due to a missing prefix.
* **Auto-Update System:** Automatically checks GitHub Releases for updates and downloads them (Configurable).
* **Smart Config:** Updates configuration files automatically without deleting your existing settings.
* **Highly Configurable:** Customize the prefix, kick messages, and toggle features.

## 📥 Installation

1. Ensure you have **Geyser** and **Floodgate** installed on your server (or proxy).
2. Download the latest `.jar` from the [Releases](https://github.com/muzaaqi/floodgate-prefix-guard/releases) page.
3. Place the file into your server's `plugins` folder.
4. Restart the server.

## ⚙️ Configuration

The `config.yml` will be generated automatically. You can customize it to fit your needs:

```yaml
# FloodgatePrefixGuard Configuration

# The prefix required for Bedrock players (must match your floodgate/config.yml)
required-prefix: "."

# Whitelist Mode / Linked Account Bypass
# If true: Linked Bedrock players (Global Linking) can join without a prefix.
# This is safe because linked players use the correct Java UUID.
allow-linked-bypass: true

# Staff Notifications
# If true: Sends a message to players with 'fpg.notify' permission when someone is kicked.
staff-notify: true
staff-notify-message: "&c[Guard] &e%player% &7was kicked due to missing prefix."

# Auto Update
# If true: Automatically downloads the latest version from GitHub.
auto-update: true

# Kick Message
# Supports color codes (&)
kick-message:
  - "&c&l[Guard] Invalid Bedrock Identity"
  - ""
  - "&7System detected a synchronization error with your name prefix."
  - "&7To prevent data loss (UUID Conflict), your connection was blocked."
  - ""
  - "&eSolution: Please try rejoining the server."

```

## 📜 Commands & Permissions

| Command | Permission | Description |
| --- | --- | --- |
| `/fpg reload` | `fpg.admin` | Reloads the configuration file without restarting the server. |

| Permission | Description |
| --- | --- |
| `fpg.notify` | Receives broadcast messages when a player is kicked. |

## 🛠️ Building from Source

To build this project, you need **Java 17** (or newer) and **Maven**.

1. Clone the repository:
```bash
git clone https://github.com/muzaaqi/floodgate-prefix-guard.git

```


2. Navigate to the project directory:
```bash
cd floodgate-prefix-guard

```


3. Build with Maven:
```bash
mvn clean install

```


4. The output JAR will be in the `target/` directory.

## 🤝 Contributing

Contributions are welcome! Please submit a Pull Request or open an Issue if you find any bugs or have feature suggestions.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](https://www.google.com/search?q=LICENSE) file for details.