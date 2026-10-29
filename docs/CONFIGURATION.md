# Configuration reference

Only message presentation is configurable. There are no database credentials,
provider-selection overrides or economy-storage settings.

- `config-version`: currently `1`.
- `prefix`: optional text, expanded only at `%prefix%`.
- `messages`: strings of at most 8192 characters. An empty string hides that message.
- Colors use `&a` and `&#RRGGBB` (legacy/HEX), not MiniMessage.
- Newlines may be literal YAML escapes or `\n` in the resulting string. Use single
  quotes for a literal backslash, or `\\n` inside a double-quoted YAML string.

| Message | Available placeholders |
| --- | --- |
| `header` | `%version%` |
| `service-line` | `%service%`, `%provider%`, `%state%` |
| `provider-line` | `%selected%`, `%provider%`, `%plugin%`, `%priority%`, `%state%` |
| All templates | `%prefix%` |

`selected`, `not-found`, `unknown-provider`, `unknown-owner` and `state-*` are
editable labels. Values substituted into a template are not recursively expanded
as more placeholders. PlaceholderAPI is not required or invoked.

After editing, use `/rzvault reload`. This reads and validates the complete file
before applying it. An invalid value retains the last valid snapshot and reports
the reason to the console. This command does not replace a JAR, restart provider
plugins, or reset temporary permissions. Restart the server for plugin updates.

The English default is in `src/main/resources/config.yml`; the Russian example
is `docs/config.ru.yml`. To switch, copy the desired message values into your
server's `plugins/rzVault/config.yml`. Existing files are never translated or
rewritten automatically.
