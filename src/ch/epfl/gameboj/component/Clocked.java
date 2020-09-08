package ch.epfl.gameboj.component;

public interface Clocked {

    /**
     *  demande au composant d'évoluer 
     *  en exécutant toutes les opérations 
     *  qu'il doit exécuter durant le 
     *  cycle d'index donné en argument
     * @param cycle
     */
    public abstract void cycle(long cycle);
        
}
