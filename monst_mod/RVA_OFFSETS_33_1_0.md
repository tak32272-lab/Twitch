# Monster Strike v33.1.0 - RVA Offset List (arm64)
## libmonsterstrike.so Static Binary Patch

### 1. ワンパン (One-Punch Kill)
| Function | RVA | Patch |
|---|---|---|
| `TaskEnemy::addDamage` | `0x04A91D7C` | B → code cave (damage × 32767) |
| Code cave | `0x09F00000` | MOV W9,#0x7FFF; MUL W1,W1,W9; orig; B back |

### 2. SS無限 (Infinite Strike Shot)
| Function | RVA | Patch |
|---|---|---|
| `strikeShotEnable` | `0x075C2F08` | MOV W0,#1; RET |

### 3. 無敵 (Invincibility)
| Function | RVA | Patch |
|---|---|---|
| `checkGameOver` | `0x04C15198` | RET |
| `trueDamageToCharBall` | `0x061402D4` | RET |
| `sumCharballDamages` | `0x049634BC` | MOV W0,#0; RET |

### 4. 回復常時 (Constant HP Recovery)
| Function | RVA | Patch |
|---|---|---|
| `getAbilityRecoverHpRate` | `0x072CD744` | MOV W0,#100; RET |

### 5. Fantastic常時 (Always Perfect Shot)
| Function | RVA | Patch |
|---|---|---|
| `SysGameManager::decisionPerfectShot` | `0x0574143C` | MOV W0,#1; RET |
| `SysGameManager::OneHandShot::decisionPerfectShot` | `0x060E40A0` | MOV W0,#1; RET |

### 6. クリティカル常時 (Always Critical)
| Function | RVA | Patch |
|---|---|---|
| `isCriticalUnabledInStrikeShot` | `0x05C611AC` | MOV W0,#0; RET |
| `strikeShotEnableChangeHitType` | `0x0496310C` | MOV W0,#1; RET |

### 7. 反射→貫通 (Reflection → Penetration)
| Function | RVA | Patch |
|---|---|---|
| `isOKWallThrough` | `0x05A4B5D4` | MOV W0,#1; RET |
| `isOKWallThroughV2` | `0x06AE6234` | MOV W0,#1; RET |

### 8. 検知バイパス (Detection Bypass)
| Function | RVA | Patch |
|---|---|---|
| `checkIntegrity1` | `0x04967E1C` | MOV W0,#0; RET |
| `checkIntegrity2` | `0x049680A8` | MOV W0,#0; RET |
| `checkIntegrity3` | `0x05741A70` | MOV W0,#0; RET |
| `checkIntegrity4` | `0x05741C08` | MOV W0,#0; RET |
| `checkIntegrity5` | `0x074F5B90` | MOV W0,#0; RET |
| `checkIntegrity6` | `0x074F5D28` | MOV W0,#0; RET |

---
**ARM64 Instruction Reference:**
- `MOV W0,#1` = `0x52800020`
- `MOV W0,#0` = `0x52800000`
- `RET` = `0xD65F03C0`
- `NOP` = `0xD503201F`
- `MOV W0,#100` = `0x52800C80`
- `MOV W9,#0x7FFF` = `0x528FFFE9`
- `MUL W1,W1,W9` = `0x1B097C21`
