Revo UI flight instrument themes (schema 3)

Place *.json files here. UIE checks them once per second while flying.
Select a loaded id from Revo UI -> Flight Roll -> HUD theme.

Built-ins:
  airbus-a319   compact A319 MPP-specific symbology
  airbus-a350   A350 dual-HUD-specific symbology
  boeing-737    737 HGS-4000 moving heading card and number drums
  airbus-a319-360, airbus-a350-360, boeing-737-360
                expanded full-sphere pitch-ladder variants
  msfs-external Microsoft Flight Simulator-style external instruments
  fpv-racing    dense racing-quad OSD
  fpv-freestyle minimal freestyle-quad OSD
  fpv-long-range navigation-oriented long-range OSD
  stereotype-tactical deliberately cinematic tactical HUD

Top-level fields:
  schema (must be 3), id, name, style
  canvasWidth, canvasHeight, crosshairMode (KEEP or HIDE_VANILLA), lineWidth, textScale,
  colors, stall, elements

"elements" creates the HUD. Array order is draw order. Every element accepts:
  id, type, enabled, x, y, scale, color
Additional geometry/data fields are type dependent:
  width, height, radius, range, majorStep, minorStep, boxWidth,
  trendSeconds, decimals, label, variant,
  pitchPixelsPerDegree, driftPixelsPerDegree, pitchRange, pitchStep,
  pitchMode (LIMITED or WRAP_360), showBankScale, showFlightPathVector,
  showEnergyCue, showAircraftReference

Element types:
  STATUS, FLIGHT_REFERENCE, AIRSPEED_TAPE, ALTITUDE_TAPE, VERTICAL_SPEED,
  HEADING_RIBBON, HEADING_ARC, HEADING_DIAL, GROUND_SPEED, DATUM,
  INPUT_STICK, AOA_GAUGE, ENERGY_GAUGE

Model variants:
  AIRBUS on STATUS/FLIGHT_REFERENCE
  AIRBUS_SPEED_POINTER and AIRBUS_ALTITUDE_DRUM on tapes
  BOEING on STATUS/FLIGHT_REFERENCE; BOEING_DRUM on tapes
  MSFS on STATUS; MSFS_LEFT/MSFS_RIGHT on tapes

Coordinates use the theme canvas. INPUT_STICK is an ordinary JSON element, so its
position and size are fully configurable. The complete virtual canvas is fitted
proportionally into the current game-window safe area every frame. Omit a component
to avoid creating it.
Use "extends" to inherit a built-in. Child element objects with the same id patch
the inherited component; a new id appends a component. See example-airliner-hud.json.

Colors use #RRGGBB or #AARRGGBB. Common keys are primary, secondary, horizon,
flightPath, energy, reference, selected, safe, warning, panel, panelBorder, halo.

Display units remain player settings. Tape ranges/steps are authored in knots and
feet, and VERTICAL_SPEED range is feet/minute; UIE converts to the selected units.
The VLS/MIN line derives from Minecraft 1.12 Elytra coefficients and is a gameplay
warning reference, not a real-aircraft angle-of-attack simulation.
