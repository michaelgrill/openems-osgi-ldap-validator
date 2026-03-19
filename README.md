# IntelliJ LDAP Filter Validator Plugin

An IntelliJ IDEA plugin that validates LDAP filter strings used inside `@Reference` annotations, helping you catch errors early and improve navigation within your code.

---

## ✨ Features

### 🔍 LDAP Filter Validation

* Detects and highlights **invalid characters** in LDAP filter strings.
* Provides immediate feedback while editing, reducing runtime errors.

### 🔗 Variable Resolution & Navigation

* Supports variable references such as:

  ```java
  @Reference(target = "(&(enabled=true)(!(id=${config.component_id})))")
  ```
* Allows you to:

  * Navigate to referenced variables (e.g. `${config.alias}`)
  * Validate whether the referenced configuration actually exists

### 🎨 Syntax Highlighting

* Applies general character highlighting to improve readability of LDAP filters.
* Makes complex filter expressions easier to understand at a glance.

---

## 🚀 Example

```java
@Reference(target = "(&(enabled=true)(!(service.factoryPid=${config.alias})))")
```

With the plugin enabled, you get:

* Real-time validation of the LDAP filter syntax
* Highlighting of invalid or suspicious characters
* Navigation support for `${config.alias}`

---

## 🛠 Installation

1. Build the plugin using Gradle:

   ```bash
   ./gradlew buildPlugin
   ```
2. In IntelliJ IDEA:

   * Go to **Settings / Preferences**
   * Navigate to **Plugins**
   * Click **⚙️ → Install Plugin from Disk**
   * Select the generated `.zip` file

---

## 📌 Use Cases

* OSGi development with `@Reference` annotations
* Projects using dynamic LDAP filter expressions
* Developers who want safer, more readable configuration strings

---

## 🔮 Future Improvements (Ideas)

* Full LDAP syntax validation (nested expressions, operators)
* Auto-completion for common LDAP attributes
* Quick fixes for common mistakes
* Support for additional annotation types

---

## 🤝 Contributing

Contributions are welcome! Feel free to open issues or submit pull requests to improve functionality, add features, or fix bugs.

---

## 💡 Motivation

LDAP filter strings can be difficult to read and easy to break. This plugin aims to bring better tooling support directly into IntelliJ, improving developer productivity and reducing configuration errors.
