package de.julielab.jcore.reader.xmi;

/**
 * This interface primarily allows to use the same initialization code for {@link XmiDBReader} and {@link XmiDBMultiplier}
 * through an instance of {@link Initializer}. The two methods it defines are necessary because they deliver information
 * that is now known to the multiplier during initialization time and thus cannot be added to the constructor of the
 * initializer.
 */
public interface Initializable {

    String[] getAdditionalTableNames();
    String[] getTables();
}
