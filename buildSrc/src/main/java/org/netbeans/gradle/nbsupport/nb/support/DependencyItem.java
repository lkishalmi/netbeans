package org.netbeans.gradle.nbsupport.nb.support;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 *
 * @author lkishalmi
 */
public class DependencyItem<T> {
    
    Set<DependencyItem<? extends T>> dependencies = new LinkedHashSet<>();
    final String name;

    public DependencyItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    public void dependsOn(DependencyItem<? extends T> dependency) {
        dependencies.add(dependency);
    }
    
    public Set<DependencyItem<? extends T>> getDependencies(boolean transitive) throws IllegalStateException {
        Set<DependencyItem<? extends T>> ret = new HashSet<>();
        if (!transitive) {
            ret.addAll(dependencies);
        } else {
            LinkedList<DependencyItem<? extends T>> todo = new LinkedList<>();
            todo.add(this);
            while (!todo.isEmpty()) {
                DependencyItem<? extends T> last = todo.peekLast();
                if (ret.contains(last)) {
                    todo.removeLast();
                } else {
                    for (DependencyItem<? extends T> dep : last.dependencies) {
                        if (!todo.contains(dep)) {
                            todo.add(dep);
                        } else {
                            throw new IllegalStateException("There is a loop in the dependency graph on: " + dep.name);
                        }
                    }
                    ret.add(last);
                }
                
            }
            ret.remove(this);
        }
        return ret;
    }

    public Set<DependencyItem<? extends T>> getDependencies() {
        return dependencies;
    }
    
    @Override
    public String toString() {
        return "DependencyItem{" + "name=" + name + '}';
    }
    
    
}
