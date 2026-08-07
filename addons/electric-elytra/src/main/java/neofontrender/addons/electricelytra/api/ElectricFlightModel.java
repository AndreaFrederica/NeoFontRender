package neofontrender.addons.electricelytra.api;

/** Declares which movement solver owns an aircraft exposed through the Electric Elytra API. */
public enum ElectricFlightModel {
    /** Minecraft's original Elytra travel solver plus optional additive electric thrust. */
    VANILLA_ELYTRA,
    /** A complete aerodynamic solver, such as Revo's powered flight-wing implementation. */
    AERODYNAMIC,
    /** Physics is fully owned by the provider; the API supplies discovery and telemetry only. */
    EXTERNAL
}
