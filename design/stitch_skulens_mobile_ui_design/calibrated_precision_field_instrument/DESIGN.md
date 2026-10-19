---
name: Calibrated Precision Field Instrument
colors:
  surface: '#f2fbfa'
  surface-dim: '#d3dcda'
  surface-bright: '#f2fbfa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#ecf5f4'
  surface-container: '#e7f0ee'
  surface-container-high: '#e1eae9'
  surface-container-highest: '#dbe4e3'
  on-surface: '#151d1d'
  on-surface-variant: '#3f4945'
  inverse-surface: '#293231'
  inverse-on-surface: '#eaf3f1'
  outline: '#707975'
  outline-variant: '#bfc9c4'
  surface-tint: '#29695b'
  primary: '#00342b'
  on-primary: '#ffffff'
  primary-container: '#004d40'
  on-primary-container: '#7ebdac'
  inverse-primary: '#94d3c1'
  secondary: '#00696a'
  on-secondary: '#ffffff'
  secondary-container: '#9dedee'
  on-secondary-container: '#0b6e6f'
  tertiary: '#003338'
  on-tertiary: '#ffffff'
  tertiary-container: '#004b52'
  on-tertiary-container: '#5dbecb'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#afefdd'
  primary-fixed-dim: '#94d3c1'
  on-primary-fixed: '#00201a'
  on-primary-fixed-variant: '#065043'
  secondary-fixed: '#a0f0f1'
  secondary-fixed-dim: '#84d4d4'
  on-secondary-fixed: '#002020'
  on-secondary-fixed-variant: '#004f50'
  tertiary-fixed: '#92f1fe'
  tertiary-fixed-dim: '#75d5e2'
  on-tertiary-fixed: '#001f23'
  on-tertiary-fixed-variant: '#004f56'
  background: '#f2fbfa'
  on-background: '#151d1d'
  surface-variant: '#dbe4e3'
typography:
  display-lg:
    fontFamily: Manrope
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
  headline-lg:
    fontFamily: Manrope
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  headline-lg-mobile:
    fontFamily: Manrope
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  headline-md:
    fontFamily: Manrope
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-lg:
    fontFamily: Manrope
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.5px
  code-sku:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 26px
    letterSpacing: 1.2px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-sm: 0.5rem
  margin: 1rem
  margin-tablet: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

The design system establishes a focused, high-reliability field instrument tailored for rugged logistics environments, intense warehouse glare, and remote field verification. The visual aesthetic fuses Material Design 3 ergonomics with utilitarian optical precision. Instead of sterile enterprise software or consumer gadget styling, the interface reflects industrial calibration: quiet confidence, immediate feedback, and ultra-high legibility.

Key emotional traits include:
- **Calibrated Rigor**: Sharp, purposeful contrast ratios ensuring unambiguous recognition across variable lighting conditions.
- **Utilitarian Speed**: Tactile clarity with oversized tap zones designed for rapid one-handed gloved operation.
- **Architectural Calm**: Grounded deep pine-teal tones anchored against structured cool-tinted surfaces, removing visual fatigue during continuous verification workflows.

## Colors

The color architecture enforces absolute contrast and clear status hierarchies:

- **Primary (`#004D40`)**: Pine-teal anchor used for core active states, confirmed scan triggers, and verified telemetry badges. Delivers authoritative presence on bright daylight screens.
- **Secondary (`#006A6B`) & Tertiary (`#00838F`)**: Complementary optical cyan tones dedicated to scanning states, viewfinder HUD geometry, and secondary action clusters.
- **Surfaces & Neutrals**: 
  - `Surface`: `#F8FAF9` (Main canvas, neutral-cool base)
  - `Surface Container`: `#EDF1F0` (Structural cards, list containers)
  - `Surface Container High`: `#E1E6E5` (Active card states, input wells)
  - `On-Surface / Text`: `#191C1C` (Deepest neutral for non-glare typography)
  - `Outline`: `#707978` (Muted functional boundaries, hairline card strokes)
- **Functional States**:
  - `Success`: `#1B6E45` on `#D1E8D5` (SKU match confirmed, vector verified)
  - `Warning`: `#8D5300` on `#FFE082` (Low confidence match, barcode partially obscured)
  - `Error`: `#BA1A1A` on `#FFDAD6` (Unrecognized item, sync timeout)

## Typography

Typography pairs **Manrope** for primary navigation titles and large metrics with **Inter** for data grids, body copy, and status indicators.

- **SKU & Serial Codes (`code-sku`)**: Formatted using Inter Bold with positive letter spacing (`1.2px`) and tabular numbers to facilitate rapid alphanumeric double-checks without character misinterpretation (such as `0` vs `O`, `1` vs `I`).
- **Headlines**: Set in Manrope with geometric clarity to anchor screen titles during swift terminal scrolling.
- **Body & Labels**: Prioritize dense line heights and distinct font weights to ensure instant visual decoding under fluorescent or harsh overhead lighting.

## Layout & Spacing

The layout utilizes an adaptive fluid grid optimized primarily for standard Android handheld form factors and rugged barcode-scanner terminals:

- **Mobile Grid**: 4 columns with `16px` (`1rem`) margins and `16px` gutters. Provides edge clearance for rugged protective phone cases.
- **Tablet & Vehicle Mount Grid**: 8 columns with `24px` (`1.5rem`) margins and `16px` gutters. Enables side-by-side viewfinder and matched results panels.
- **Touch Ergonomics**: All actionable buttons and scan triggers maintain a minimum vertical touch footprint of 56px (`3.5rem`), supporting single-hand thumb reach across the lower 40% of the display.

## Elevation & Depth

Visual hierarchy relies on Material Design 3 surface color stepping and hairline technical outlines rather than heavy drop shadows:

- **Surface Tonal Stacking**:
  - `Base`: Canvas is `#F8FAF9`.
  - `Resting Cards`: Rendered in `#EDF1F0` bordered by a 1px solid `#707978` stroke at 20% opacity.
  - `Hover / Pressed / Raised`: Surfaces transition to `#E1E6E5` with an ambient shadow (`y: 2px, blur: 8px, rgba(0, 77, 64, 0.08)`).
- **Scanner Viewfinder HUD**: Floating viewfinder brackets and live camera HUD elements sit on pure dark neutral overlays (`#000000` at 40% opacity) with crisp, unblurred borders. Heavy glows and soft glassmorphism are explicitly avoided to guarantee zero render-thread lag on low-power devices.

## Shapes

The interface balances functional softness and technical precision:

- **Cards & Modal Sheets**: Styled with `rounded-lg` (16px / 1rem) corner radiuses, conforming to modern Material Design 3 specifications.
- **Buttons, Badges & Chips**: Styled with fully rounded capsules (28px height or 56px height for primary triggers) to signal tap affordances instantly.
- **HUD Reticles**: Hard 4px brackets framing the camera viewport, conveying optical calibration.

## Components

### Buttons & Action Controls
- **Primary Capture CTA**: Height 56dp, full width or floating bottom-center pill. Background `#004D40`, label `#FFFFFF` in `label-lg`. Focus state adds an outer 3dp ring in `#00838F`.
- **Secondary Button**: Outlined 48dp pill. Border 1.5dp `#707978`, label `#004D40`.
- **Segmented Tab Row ("Ara" / "Ekle")**: Contained pill container (`#EDF1F0`), 48dp height. Active segment uses `#004D40` with `#FFFFFF` text; inactive segment uses `#191C1C` with transparent background.

### Input Fields & Search
- **Text Inputs ("Barkod / SKU Girin")**: 56dp height, background `#EDF1F0`, 12dp rounded corners. Border 1px `#707978` resting, shifting to 2dp `#004D40` on focus. SKU mono-spacing applied automatically.
- **Clear & Flash Toggles**: 44dp circular icon buttons embedded inside input and HUD areas.

### Cards & Result Lists
- **SKU Result Card**: 16dp rounded corner container (`#EDF1F0`). Houses thumbnail (48x48dp, 8dp radius), `code-sku` header, confidence score chip, and inventory metadata.
- **Status Indicator Badges**: 24dp pills with paired tonal backgrounds:
  - Match Confirmed: `#D1E8D5` container with `#1B6E45` text ("Eşleşti").
  - Vector Uncertainty: `#FFE082` container with `#8D5300` text ("Doğrulama Gerekli").
  - Mismatch: `#FFDAD6` container with `#BA1A1A` text ("Bulunamadı").

### Camera Viewfinder & HUD
- Centered rectangular scanning reticle with 24dp length, 3dp stroke corner brackets in `#00838F`.
- Real-time offline vector match indicator: Compact floating capsule at reticle top reading "Çevrimdışı Model Aktif" (Offline Model Active) with an emerald telemetry dot.