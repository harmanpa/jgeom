/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package math.geom2d;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 *
 * @author peter
 */
public class Range1D implements Comparable<Range1D> {

    private final double min;
    private final double max;

    public Range1D(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public static Range1D fromStatistics(DoubleSummaryStatistics dss) {
        return new Range1D(dss.getMin(), dss.getMax());
    }

    public static Range1D fromValues(DoubleStream values) {
        return fromStatistics(values.summaryStatistics());
    }

    public static Range1D bounds(Shape2D shape, Vector2D direction) {
        Box2D box = shape.transform(AffineTransform2D.createRotation(Angle2D.angle(direction.normalize(), new Vector2D(0, 1))))
                .boundingBox();
        return new Range1D(box.getMinY(), box.getMaxY());
    }

    public double getMin() {
        return min;
    }

    @JsonIgnore
    public double getMid() {
        return (getMin() + getMax()) / 2.0;
    }

    @JsonIgnore
    public double getLength() {
        return getMax() - getMin();
    }

    public double getMax() {
        return max;
    }

    /**
     * Returns true if the ranges overlap
     *
     * @param other
     * @return
     */
    public boolean isOverlapping(Range1D other) {
        return this.getMax() >= other.getMin() && this.getMin() <= other.getMax();
    }

    /**
     * Calculates the smallest gap between the ranges, or a negative value for
     * the smallest overlap
     *
     * @param other
     * @return
     */
    public double distance(Range1D other) {
        if (getMax() <= other.getMin()) {
            return other.getMin() - getMax();
        } else if (getMin() >= other.getMax()) {
            return getMin() - other.getMax();
        } else {
            return -1 * overlap(other);
        }
    }

    /**
     * Calculate the overlap between the ranges, or 0.0 if they do not overlap
     *
     * @param other
     * @return
     */
    public double overlap(Range1D other) {
        if (getMax() <= other.getMin() || getMin() >= other.getMax()) {
            return 0.0;
        }
        double overlapMax = Math.min(getMax(), other.getMax());
        double overlapMin = Math.max(getMin(), other.getMin());
        return Math.max(overlapMax - overlapMin, 0.0);
    }

    @Override
    public int compareTo(Range1D other) {
        if (getMax() <= other.getMin()) {
            return -1;
        } else if (getMin() >= other.getMax()) {
            return 1;
        } else {
            return 0;
        }
    }

    static final Comparator<Range1D> MINCOMPARATOR = (Range1D o1, Range1D o2) -> Double.compare(o1.getMin(), o2.getMin());

    static final BiFunction<Deque<Range1D>, Range1D, Deque<Range1D>> ACCUMULATOR = (Deque<Range1D> queue, Range1D range) -> {
        if (queue.isEmpty() || queue.peekLast().getMax() < range.getMin()) {
            queue.addLast(range);
        } else {
            Range1D last = queue.pollLast();
            queue.addLast(new Range1D(last.getMin(), range.getMax()));
        }
        return queue;
    };

    static final BinaryOperator<Deque<Range1D>> COMBINER = (Deque<Range1D> set1, Deque<Range1D> set2) -> {
        set2.forEach(r -> ACCUMULATOR.apply(set1, r));
        return set1;
    };

    /**
     * Create the resulting ranges from subtracting the given range from this
     *
     * @param range
     * @return
     */
    public Collection<Range1D> subtract(Range1D range) {
        return subtract(Arrays.asList(range));
    }

    /**
     * Create the resulting ranges from subtracting the given ranges from this
     *
     * @param ranges
     * @return
     */
    public Collection<Range1D> subtract(Collection<Range1D> ranges) {
        return subtract(ranges.stream());
    }

    /**
     * Create the resulting ranges from subtracting the given ranges from this
     *
     * @param ranges
     * @return
     */
    public Collection<Range1D> subtract(Stream<Range1D> ranges) {
        // Merge the ranges in case they overlap each other, and filter to those overlapping this range
        List<Range1D> overlapping = merge(ranges).stream()
                .filter(range -> isOverlapping(range))
                .collect(Collectors.toList());
        // If there are none just return this
        if (overlapping.isEmpty()) {
            return Arrays.asList(this);
        }
        List<Range1D> out = new ArrayList<>();
        // Consider whether ranges are needed before and after the subtracted ones
        boolean firstIsContained = overlapping.get(0).getMin() > getMin();
        boolean lastIsContained = overlapping.get(overlapping.size() - 1).getMax() < getMax();
        if (firstIsContained) {
            out.add(new Range1D(getMin(), overlapping.get(0).getMin()));
        }
        for (int i = 1; i < overlapping.size(); i++) {
            out.add(new Range1D(overlapping.get(i - 1).getMax(), overlapping.get(i).getMin()));
        }
        if (lastIsContained) {
            out.add(new Range1D(overlapping.get(overlapping.size() - 1).getMax(), getMax()));
        }
        return out;
    }

    /**
     * Combine a Collection of ranges by merging any that overlap
     *
     * @param ranges
     * @return
     */
    public static Collection<Range1D> merge(Collection<Range1D> ranges) {
        return merge(ranges.stream());
    }

    /**
     * Combine a Stream of ranges by merging any that overlap
     *
     * @param ranges
     * @return
     */
    public static Collection<Range1D> merge(Stream<Range1D> ranges) {
        return ranges.sequential().sorted(MINCOMPARATOR).reduce(new ArrayDeque<>(), ACCUMULATOR, COMBINER);
    }

    public static Collection<Range1D> union(Collection<Collection<Range1D>> allRanges) {
        return merge(allRanges.stream().flatMap(c -> c.stream()));
    }

    public static Collection<Range1D> intersect(Collection<Collection<Range1D>> allRanges) {
        return allRanges.stream().reduce(Range1D::intersect).orElseGet(() -> Arrays.asList());
    }

    public static Collection<Range1D> union(Collection<Range1D> rangesA, Collection<Range1D> rangesB) {
        return union(Arrays.asList(rangesA, rangesB));
    }

    public static Collection<Range1D> intersect(Collection<Range1D> rangesA, Collection<Range1D> rangesB) {
        if (rangesA.isEmpty() || rangesB.isEmpty()) {
            return Arrays.asList();
        }
        List<Range1D> mergedA = merge(rangesA).stream().collect(Collectors.toList());
        Range1D outer = new Range1D(mergedA.get(0).getMin(), mergedA.get(mergedA.size() - 1).getMax());
        Collection<Range1D> intermediate = outer.subtract(mergedA);
        return merge(merge(rangesB).stream().flatMap(range -> range.subtract(intermediate).stream()));
    }

}
