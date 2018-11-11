package io.dico.dicore.task;

import java.util.NoSuchElementException;

public abstract class VoidTask extends BaseTask<Void> {
    
    @Override
    protected final boolean process(Void object) {
        return process();
    }
    
    @Override
    protected final Void supply() throws NoSuchElementException {
        return null;
    }
    
    protected abstract boolean process();
    
}
