# BedrockGUI Designer UI Style Guide

## Goals
- Consistent, high-contrast UI across the app
- Predictable component sizing and spacing (8px grid)
- Accessibility by default (keyboard + focus + readable text)

## Color System
This project uses CSS variables as design tokens, mapped into Tailwind under `brand.*`.

**Tokens**
- `--ui-bg` page background
- `--ui-surface` panel background
- `--ui-surface-2` elevated inputs/cards background
- `--ui-border` borders/dividers
- `--ui-text` main text color
- `--ui-muted` secondary text color
- `--ui-accent` primary action color
- `--ui-accent-text` text on accent backgrounds
- `--ui-focus` focus ring color

Defined in: [styles.css](file:///c:/Users/pintu/Desktop/Server/BedrockGUI/designer-app/src/styles.css)

## Spacing
- Base spacing uses an 8px rhythm.
- Minimum spacing between interactive controls: 8px.

## Buttons
Buttons are standardized via component classes in `styles.css`.

**Classes**
- `ui-btn-primary`
- `ui-btn-secondary`
- `ui-btn-ghost`

**States**
- Hover: subtle brightness/background change
- Disabled: reduced opacity + not-allowed cursor
- Focus: `:focus-visible` outline via `--ui-focus`

## Inputs
Standardized via:
- `ui-input`
- `ui-textarea`

## Panels
Standardized via:
- `ui-panel`
- `ui-panel-title`
- `ui-chip`

## Form Mockups
The app includes a live panel that renders mockups/examples for:
- Bedrock SIMPLE / MODAL / CUSTOM
- Java CHEST / ANVIL

Location: `UI Guide` panel in the left sidebar.

## Accessibility Checklist
- Tab navigation reaches all interactive elements
- Visible focus ring (`:focus-visible`)
- Buttons use `<button>` with clear labels where possible
- Avoid color-only meaning; pair color with text or shape

## Before/After Summary
**Before**
- Inconsistent grays, mixed paddings/radii, uneven button sizing

**After**
- Token-driven colors, standardized button/input/panel styles, consistent spacing and focus ring

