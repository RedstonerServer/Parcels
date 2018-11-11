package io.dico.dicore;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SetBasedWhitelist implements Whitelist {
    private final Set set;
    private final boolean blacklist;
    
    public SetBasedWhitelist(Object[] array, boolean blacklist) {
        this(Arrays.asList(array), blacklist);
    }
    
    public SetBasedWhitelist(ConfigurationSection section, Function<String, ?> parser) {
        this(section.getStringList("listed").stream().map(parser).filter(Objects::nonNull).collect(Collectors.toList()),
                section.getBoolean("blacklist", false));
    }
    
    @SuppressWarnings("unchecked")
    public SetBasedWhitelist(Collection collection, boolean blacklist) {
        Set set;
        if (collection.isEmpty()) {
            set = Collections.emptySet();
        } else if (collection.iterator().next() instanceof Enum) {
            set = EnumSet.copyOf(collection);
        } else if (collection instanceof Set) {
            set = (Set) collection;
        } else {
            set = new HashSet<>(collection);
        }
        
        this.set = set;
        this.blacklist = blacklist;
    }
    
    @Override
    public boolean isWhitelisted(Object o) {
        return blacklist != set.contains(o);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public String toString() {
        return (blacklist ? "Blacklist" : "Whitelist") + "{"
                + String.join(", ", (CharSequence[]) set.stream().map(String::valueOf).toArray(String[]::new)) + "}";
    }
    
    public Set getSet() {
        return set;
    }
    
    public boolean isBlacklist() {
        return blacklist;
    }
    
}
