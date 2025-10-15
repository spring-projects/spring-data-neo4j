---
name: Bug report
about: Create a report to help us improve Spring Data FalkorDB
title: ''
labels: 'bug'
assignees: ''

---

## 🐛 Bug Description
A clear and concise description of what the bug is.

## 🔄 Steps to Reproduce
Steps to reproduce the behavior:
1. Set up FalkorDB with '...'
2. Create entity with '...'
3. Execute operation '...'
4. See error

## ✅ Expected Behavior
A clear and concise description of what you expected to happen.

## ❌ Actual Behavior
A clear and concise description of what actually happened.

## 📊 Environment
- **Spring Data FalkorDB version**: [e.g., 1.0.0-SNAPSHOT]
- **FalkorDB version**: [e.g., latest, v4.0.9]
- **Java version**: [e.g., OpenJDK 17]
- **Spring Boot version** (if applicable): [e.g., 3.2.0]
- **Operating System**: [e.g., Ubuntu 20.04, macOS 13, Windows 11]

## 📋 Code Sample
```java
// Minimal code sample that reproduces the issue
@Node("Person")
public class Person {
    @Id
    @GeneratedValue
    private Long id;
    // ...
}
```

## 📝 Error Logs
```
Paste any relevant error logs here
```

## 💡 Additional Context
Add any other context about the problem here, such as:
- Does this happen with specific graph structures?
- Is this related to relationship mapping?
- Does this occur only with certain query patterns?

## 🔍 Possible Solution (Optional)
If you have ideas on how to fix this, please share them here.