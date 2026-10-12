package fr.luuuccasss.discrimination.client;

import fr.luuuccasss.discrimination.Prejudice;

/** Préjugé transporté dans le render state du villageois (1.21.2+). */
public interface AccessoryRenderState {
    Prejudice discrimination$getPrejudice();

    void discrimination$setPrejudice(Prejudice prejudice);
}
