/*
 * Licensed to the Ted Dunning under one or more contributor license
 * agreements.  See the NOTICE file that may be
 * distributed with this work for additional information
 * regarding copyright ownership.  Ted Dunning licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.mapr.synth;

import com.google.common.base.Function;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import org.apache.commons.math3.distribution.NormalDistribution;
import com.mapr.synth.distributions.LongTail;
import com.mapr.synth.distributions.TermGenerator;
import com.mapr.synth.distributions.WordGenerator;
import org.apache.mahout.math.stats.LogLikelihood;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TermGeneratorTest {

    private static final WordGenerator WORDS = new WordGenerator("word-frequency-seed", "other-words");

    @Test
    public void generateTerms() {
        TermGenerator x = new TermGenerator(WORDS, 1, 0.8);
        final Multiset<String> counts = HashMultiset.create();
        for (int i = 0; i < 10000; i++) {
            counts.add(x.sample());
        }

        assertEquals(10000, counts.size());
        assertTrue("Should have some common words", counts.elementSet().size() < 10000);
        List<Integer> k = counts.elementSet().stream()
                .map(counts::count).collect(Collectors.toList());
//        System.out.printf("%s\n", Ordering.natural().reverse().sortedCopy(k).subList(0, 30));
//        System.out.printf("%s\n", Iterables.transform(Iterables.filter(counts.elementSet(), new Predicate<String>() {
//            public boolean apply(String s) {
//                return counts.count(s) > 100;
//            }
//        }), new Function<String, String>() {
//            public String apply(String s) {
//                return s + ":" + counts.count(s);
//            }
//        }));
        assertEquals(1, Ordering.natural().leastOf(k, 1).get(0).intValue());
        assertTrue(Ordering.natural().greatestOf(k, 1).get(0) > 300);
        assertTrue(counts.count("the") > 300);
    }

    @Test
    public void distinctVocabularies() {
        TermGenerator x1 = new TermGenerator(WORDS, 10, 0.2);
        final Multiset<String> k1 = HashMultiset.create();
        for (int i = 0; i < 50000; i++) {
            k1.add(x1.sample());
        }

        TermGenerator x2 = new TermGenerator(WORDS, 10, 0.2);
        final Multiset<String> k2 = HashMultiset.create();
        for (int i = 0; i < 50000; i++) {
            k2.add(x2.sample());
        }

        final NormalDistribution normal = new NormalDistribution();

        List<Double> scores =
                Sets.union(k1.elementSet(), k2.elementSet()).stream()
                        .map(s -> normal.cumulativeProbability(
                                LogLikelihood.rootLogLikelihoodRatio(
                                        k1.count(s), 50000 - k1.count(s),
                                        k2.count(s), 50000 - k2.count(s))))
                        .sorted(Ordering.natural())
                        .collect(Collectors.toList());
        int n = scores.size();
//        System.out.printf("%.5f, %.5f, %.5f, %.5f, %.5f, %.5f, %.5f", scores.get(0), scores.get((int) (0.05*n)), scores.get(n / 4), scores.get(n / 2), scores.get(3 * n / 4), scores.get((int) (0.95 * n)), scores.get(n - 1));
        int i = 0;
        for (Double score : scores) {
            if (i % 10 == 0) {
                System.out.printf("%.6f\t%.6f\n", (double) i / n, score);
            }

            i++;
        }
    }

    @Test
    public void speciesCounts() {
        final boolean transpose = false;

        // generate an example of species sampled on multiple days
        LongTail<Integer> terms = new LongTail<>(0.5, 0.3) {
            int max = 0;

            @Override
            protected Integer createThing() {
                return ++max;
            }
        };

        // I picked seeds to get a good illustration ... want a reasonable number of species and surprises
        terms.setSeed(2);

        Random gen = new Random(1);
        SortedSet<Integer> vocabulary = Sets.newTreeSet();
        List<Multiset<Integer>> r = Lists.newArrayList();

        for (int i = 0; i < 2000; i++) {
            double length = Math.rint(gen.nextGaussian() * 10 + 50);
            Multiset<Integer> counts = HashMultiset.create();
            for (int j = 0; j < length; j++) {
                counts.add(terms.sample());
            }
            r.add(counts);
        }

        System.out.printf("%d\n", vocabulary.size());
        for (Multiset<Integer> day : r) {
            vocabulary.addAll(day.elementSet());
            String sep = "";
            System.out.printf("%s%s", sep, vocabulary.size());
            sep = "\t";
            for (Integer s : vocabulary) {
                System.out.printf("%s%s", sep, day.count(s));
                sep = "\t";
            }
            System.out.print("\n");
        }

        Multiset<Integer> total = HashMultiset.create();
        for (Multiset<Integer> day : r) {
            for (Integer species : day.elementSet()) {
                total.add(species, day.count(species));
            }
        }
        String sep = "";
        System.out.printf("%s%s", sep, total.elementSet().size());
        sep = "\t";
        for (Integer s : vocabulary) {
            System.out.printf("%s%s", sep, total.count(s));
            sep = "\t";
        }
        System.out.print("\n");
    }
}
