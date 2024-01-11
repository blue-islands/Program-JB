/*
 * This file is part of Program JB.
 *
 * Program JB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Program JB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Program JB. If not, see <http://www.gnu.org/licenses/>.
 */
package org.goldrenard.jb.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.goldrenard.jb.configuration.Constants;
import org.goldrenard.jb.model.Clause;
import org.goldrenard.jb.model.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripleStore {

    private static final Logger log = LoggerFactory.getLogger(TripleStore.class);

    private int idCnt = 0;
    private String name = "unknown";
    private Bot bot;
    private Map<String, Triple> idTriple = new HashMap<>();
    private Map<String, String> tripleStringId = new HashMap<>();
    private Map<String, Set<String>> subjectTriples = new HashMap<>();
    private Map<String, Set<String>> predicateTriples = new HashMap<>();
    private Map<String, Set<String>> objectTriples = new HashMap<>();

    private Map<String, Tuple> tupleMap = new ConcurrentHashMap<>();

    public TripleStore(final String name, final Bot bot) {
        this.name = name;
        this.bot = bot;
    }

    @Getter
    @Setter
    public class Triple {
        private String id;
        private String subject;
        private String predicate;
        private String object;

        public Triple(String s, String p, String o) {
            final Bot bot = TripleStore.this.bot;
            if (bot != null) {
                s = bot.getPreProcessor().normalize(s);
                p = bot.getPreProcessor().normalize(p);
                o = bot.getPreProcessor().normalize(o);
            }
            if (s != null && p != null && o != null) {
                this.subject = s;
                this.predicate = p;
                this.object = o;
                this.id = TripleStore.this.name + TripleStore.this.idCnt++;
            }
        }
    }

    public String mapTriple(final Triple triple) {
        final String id = triple.id;
        this.idTriple.put(id, triple);
        final String subject = triple.subject.toUpperCase();
        final String predicate = triple.predicate.toUpperCase();
        final String object = triple.object.toUpperCase();

        String tripleString = subject + ":" + predicate + ":" + object;
        tripleString = tripleString.toUpperCase();

        if (this.tripleStringId.keySet().contains(tripleString)) {
            return this.tripleStringId.get(tripleString); // triple already exists
        } else {
            this.tripleStringId.put(tripleString, id);

            Set<String> existingTriples = this.subjectTriples.getOrDefault(subject, new HashSet<>());
            existingTriples.add(id);
            this.subjectTriples.put(subject, existingTriples);

            existingTriples = this.predicateTriples.getOrDefault(predicate, new HashSet<>());
            existingTriples.add(id);
            this.predicateTriples.put(predicate, existingTriples);

            existingTriples = this.objectTriples.getOrDefault(object, new HashSet<>());
            existingTriples.add(id);
            this.objectTriples.put(object, existingTriples);
            return id;
        }
    }

    public String unMapTriple(Triple triple) {
        final String subject = triple.subject.toUpperCase();
        final String predicate = triple.predicate.toUpperCase();
        final String object = triple.object.toUpperCase();

        String tripleString = subject + ":" + predicate + ":" + object;

        log.debug("unMapTriple {}", tripleString);
        tripleString = tripleString.toUpperCase();
        triple = this.idTriple.get(this.tripleStringId.get(tripleString));
        log.debug("unMapTriple {}", triple);
        if (triple != null) {
            final String id = triple.id;
            this.idTriple.remove(id);
            this.tripleStringId.remove(tripleString);

            Set<String> existingTriples = this.subjectTriples.getOrDefault(subject, new HashSet<>());
            existingTriples.remove(id);
            this.subjectTriples.put(subject, existingTriples);

            existingTriples = this.predicateTriples.getOrDefault(predicate, new HashSet<>());
            existingTriples.remove(id);
            this.predicateTriples.put(predicate, existingTriples);

            existingTriples = this.objectTriples.getOrDefault(object, new HashSet<>());
            existingTriples.remove(id);
            this.objectTriples.put(object, existingTriples);
            return id;
        }
        return Constants.undefined_triple;
    }

    public Set<String> allTriples() {
        return new HashSet<>(this.idTriple.keySet());
    }

    public String addTriple(final String subject, final String predicate, final String object) {
        if (subject == null || predicate == null || object == null) {
            return Constants.undefined_triple;
        }
        final Triple triple = new Triple(subject, predicate, object);
        return this.mapTriple(triple);
    }

    public String deleteTriple(final String subject, final String predicate, final String object) {
        if (subject == null || predicate == null || object == null) {
            return Constants.undefined_triple;
        }
        if (log.isTraceEnabled()) {
            log.trace("Deleting {}:{}:{}", subject, predicate, object);
        }
        final Triple triple = new Triple(subject, predicate, object);
        return this.unMapTriple(triple);
    }

    public void printTriples() {
        for (final String x : this.idTriple.keySet()) {
            final Triple triple = this.idTriple.get(x);
            log.info("{}:{}:{}:{}", x, triple.subject, triple.predicate, triple.object);
        }
    }

    private Set<String> emptySet() {
        return new HashSet<>();
    }

    public Set<String> getTriples(final String s, final String p, final String o) {
        Set<String> subjectSet;
        Set<String> predicateSet;
        Set<String> objectSet;
        Set<String> resultSet;
        if (log.isTraceEnabled()) {
            log.trace("TripleStore: getTriples [{}] {}:{}:{}", this.idTriple.size(), s, p, o);
        }
        if (s == null || s.startsWith("?")) {
            subjectSet = this.allTriples();
        } else {
            subjectSet = this.subjectTriples.getOrDefault(s.toUpperCase(), this.emptySet());
        }

        if (p == null || p.startsWith("?")) {
            predicateSet = this.allTriples();
        } else {
            predicateSet = this.predicateTriples.getOrDefault(p.toUpperCase(), this.emptySet());
        }

        if (o == null || o.startsWith("?")) {
            objectSet = this.allTriples();
        } else {
            objectSet = this.objectTriples.getOrDefault(o.toUpperCase(), this.emptySet());
        }

        resultSet = new HashSet<>(subjectSet);
        resultSet.retainAll(predicateSet);
        resultSet.retainAll(objectSet);
        return resultSet;
    }

    public Set<String> getSubjects(final Set<String> triples) {
        final HashSet<String> resultSet = new HashSet<>();
        for (final String id : triples) {
            final Triple triple = this.idTriple.get(id);
            resultSet.add(triple.subject);
        }
        return resultSet;
    }

    public Set<String> getPredicates(final Set<String> triples) {
        final HashSet<String> resultSet = new HashSet<>();
        for (final String id : triples) {
            final Triple triple = this.idTriple.get(id);
            resultSet.add(triple.predicate);
        }
        return resultSet;
    }

    public Set<String> getObjects(final Set<String> triples) {
        final Set<String> resultSet = new HashSet<>();
        for (final String id : triples) {
            final Triple triple = this.idTriple.get(id);
            resultSet.add(triple.object);
        }
        return resultSet;
    }

    public String getSubject(final String id) {
        if (this.idTriple.containsKey(id)) {
            return this.idTriple.get(id).subject;
        }
        return "Unknown subject";
    }

    public String getPredicate(final String id) {
        if (this.idTriple.containsKey(id)) {
            return this.idTriple.get(id).predicate;
        }
        return "Unknown predicate";
    }

    public String getObject(final String id) {
        if (this.idTriple.containsKey(id)) {
            return this.idTriple.get(id).object;
        }
        return "Unknown object";
    }

    public String stringTriple(final String id) {
        final Triple triple = this.idTriple.get(id);
        return id + " " + triple.subject + " " + triple.predicate + " " + triple.object;
    }

    public void printAllTriples() {
        for (final String id : this.idTriple.keySet()) {
            log.info("{}", this.stringTriple(id));
        }
    }

    public Set<Tuple> select(final Set<String> vars, final Set<String> visibleVars, final List<Clause> clauses) {
        Set<Tuple> result = new HashSet<>();
        try {
            final Tuple tuple = this.storeTuple(new Tuple(vars, visibleVars));
            result = this.selectFromRemainingClauses(tuple, clauses);
        } catch (final Exception e) {
            log.error("Error", e);
        }
        return result;
    }

    public Clause adjustClause(final Tuple tuple, final Clause clause) {
        final Set vars = tuple.getVars();
        final String subj = clause.getSubj();
        final String pred = clause.getPred();
        final String obj = clause.getObj();
        final Clause newClause = new Clause(clause);
        if (vars.contains(subj)) {
            final String value = tuple.getValue(subj);
            if (!value.equals(Constants.unbound_variable)) {
                newClause.setSubj(value);
            }
        }
        if (vars.contains(pred)) {
            final String value = tuple.getValue(pred);
            if (!value.equals(Constants.unbound_variable)) {
                newClause.setPred(value);
            }
        }
        if (vars.contains(obj)) {
            final String value = tuple.getValue(obj);
            if (!value.equals(Constants.unbound_variable)) {
                newClause.setObj(value);
            }
        }
        return newClause;

    }

    public Tuple bindTuple(final Tuple partial, final String triple, final Clause clause) {
        final Tuple tuple = this.storeTuple(new Tuple(partial));
        if (clause.getSubj().startsWith("?")) {
            tuple.bind(clause.getSubj(), this.getSubject(triple));
        }
        if (clause.getPred().startsWith("?")) {
            tuple.bind(clause.getPred(), this.getPredicate(triple));
        }
        if (clause.getObj().startsWith("?")) {
            tuple.bind(clause.getObj(), this.getObject(triple));
        }
        return tuple;
    }

    public Set<Tuple> selectFromSingleClause(final Tuple partial, final Clause clause, final Boolean affirm) {
        final Set<Tuple> result = new HashSet<>();
        final Set<String> triples = this.getTriples(clause.getSubj(), clause.getPred(), clause.getObj());
        if (affirm) {
            for (final String triple : triples) {
                final Tuple tuple = this.bindTuple(partial, triple, clause);
                result.add(tuple);
            }
        } else if (triples.size() == 0) {
            result.add(partial);
        }
        return result;
    }

    public Set<Tuple> selectFromRemainingClauses(final Tuple partial, final List<Clause> clauses) {
        Set<Tuple> result = new HashSet<>();
        Clause clause = clauses.get(0);
        clause = this.adjustClause(partial, clause);
        final Set<Tuple> tuples = this.selectFromSingleClause(partial, clause, clause.getAffirm());
        if (clauses.size() > 1) {
            final List<Clause> remainingClauses = new ArrayList<>(clauses);
            remainingClauses.remove(0);
            for (final Tuple tuple : tuples) {
                result.addAll(this.selectFromRemainingClauses(tuple, remainingClauses));
            }
        } else {
            result = tuples;
        }
        return result;
    }

    public Tuple storeTuple(final Tuple tuple) {
        this.tupleMap.put(tuple.getName(), tuple);
        return tuple;
    }

    public String tupleGet(final String tupleName, final String varName) {
        final Tuple tuple = this.tupleMap.get(tupleName);
        return tuple == null ? Constants.default_get : tuple.getValue(varName);
    }
}
