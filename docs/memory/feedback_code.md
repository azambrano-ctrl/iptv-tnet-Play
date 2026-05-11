---
name: Feedback - Code style preferences
description: How the user wants code changes applied in this project
type: feedback
originSessionId: 948572b8-667b-4e20-934c-6bfd1076aad1
---
Don't explain changes step by step before doing them — just do it and confirm BUILD SUCCESSFUL.

**Why:** User wants fast results. They check visually (screenshots) not by reading diffs.

**How to apply:** Make changes, compile to verify, then give a short summary. Don't narrate.

---

Use Canvas drawing (not Unicode text or Material Icons Extended) for custom icons in Compose TV.

**Why:** The project doesn't have `material-icons-extended` dependency. Unicode chars look unprofessional on TV screens.

**How to apply:** Always draw icons with `Canvas + Path + drawLine/drawArc/drawRoundRect` in Kotlin Compose.

---

Always use named arguments for `androidx.tv.material3.Border(border=..., shape=...)`.

**Why:** The second positional argument is `inset: Dp`, not `shape`. Passing `RoundedCornerShape` positionally causes a compile error.

**How to apply:** Always write `Border(border = BorderStroke(...), shape = RoundedCornerShape(...))`.

---

The user compares the app to NOW+ IPTV as a design reference. Their brand is TNET play (red #E8001C → orange #FF6200), not NOW+'s colors.
