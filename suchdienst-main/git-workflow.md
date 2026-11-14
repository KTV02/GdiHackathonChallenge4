# Git Workflow

Wir verwenden einen modifizierten GitFlow-Workflow ohne separaten `develop`-Branch. Alle Entwicklungen erfolgen über Feature-Branches, die direkt von `main` abzweigen und nach Review wieder in `main` gemergt werden.

## Branch-Typen

- **feature/**: Neue Features und Funktionalitäten
- **bugfix/**: Fehlerbehebungen
- **hotfix/**: Dringende Fixes für Produktionsprobleme
- **release/**: Vorbereitung eines neuen Releases

## Workflow

1. **Feature-Branch erstellen**

   ```bash
   git checkout main
   git pull
   git checkout -b feature/your-feature-name
   ```

2. **Änderungen entwickeln und committen**

   ```bash
   git add .
   git commit -m 'feat: add some feature'
   ```

3. **Branch pushen**

   ```bash
   git push origin feature/your-feature-name
   ```

4. **Merge Request erstellen**  
   Erstellen Sie einen Merge Request auf GitLab und warten Sie auf Code Review

5. **Nach Approval: Merge in main**  
   Nach erfolgreichem Review wird der Branch in `main` gemergt und automatisch gelöscht

## Commit-Message Konvention

Wir folgen den [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): subject

body (optional)

footer (optional)
```

**Typen:**

- `feat`: Neues Feature
- `fix`: Fehlerbehebung
- `docs`: Dokumentationsänderungen
- `style`: Code-Style-Änderungen (Formatierung, etc.)
- `refactor`: Code-Refactoring
- `test`: Hinzufügen oder Aktualisieren von Tests
- `chore`: Wartungsaufgaben

**Beispiel:**

```
feat(auth): add OAuth2 authentication

Implement OAuth2 authentication flow with Google and GitHub providers.

Closes #123
```

---
