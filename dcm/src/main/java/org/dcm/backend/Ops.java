/*
 * Copyright © 2018-2019 VMware, Inc. All Rights Reserved.
 *
 * SPDX-License-Identifier: BSD-2
 */

package org.dcm.backend;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;
import com.google.ortools.util.Domain;

import java.util.Collections;
import java.util.List;

public class Ops {
    private final CpModel model;
    private final StringEncoding encoder;
    private final IntVar trueVar;
    private final IntVar falseVar;


    public Ops(final CpModel model, final StringEncoding encoding) {
        this.model = model;
        this.encoder = encoding;
        this.trueVar = model.newConstant(1);
        this.falseVar = model.newConstant(0);
    }

    public int sum(final List<Integer> data) {
        int ret = 0;
        for (final Integer d: data) {
            ret += d;
        }
        return ret;
    }

    public IntVar sumV(final List<IntVar> data) {
        long lowBound = 0;
        long upperBound = 0;
        for (final IntVar v: data) {
            lowBound += v.getBuilder().getDomain(0);
            upperBound += v.getBuilder().getDomain(v.getBuilder().getDomainCount() - 1);
        }
        final IntVar ret = model.newIntVar(lowBound, upperBound, "");
        assert ret.getBuilder().getDomainCount() != 0 : lowBound + " " + upperBound;
        model.addEquality(ret, LinearExpr.sum(data.toArray(new IntVar[0])));
        return ret;
    }

    // TODO: add test case to OpsTests
    public IntVar scalProd(final List<IntVar> variables, final List<Integer> coefficients) {
        long lowBound = 0;
        long upperBound = 0;
        for (int i = 0; i < variables.size(); i++) {
            final IntVar v = variables.get(i);
            final int c = coefficients.get(i);
            lowBound += c * v.getBuilder().getDomain(0);
            upperBound += c * v.getBuilder().getDomain(v.getBuilder().getDomainCount() - 1);
        }
        final IntVar ret = model.newIntVar(lowBound, upperBound, "");
        assert ret.getBuilder().getDomainCount() != 0 : lowBound + " " + upperBound;
        model.addEquality(ret, LinearExpr.scalProd(variables.toArray(new IntVar[0]),
                                                   coefficients.stream().mapToInt(Integer::intValue).toArray()));
        return ret;
    }

    public void increasing(final List<IntVar> data) {
        for (int i = 0; i < data.size() - 1; i++) {
            model.addLessOrEqual(data.get(i), data.get(i + 1));
        }
    }

    public IntVar exists(final List<IntVar> data) {
1        final IntVar bool = model.newBoolVar("");
        final Literal[] literals = data.toArray(new Literal[0]);
        model.addBoolOr(literals).onlyEnforceIf(bool);
        model.addBoolAnd(data.stream().map(IntVar::not).toArray(Literal[]::new)).onlyEnforceIf(bool.not());
        return bool;
    }

    public int maxVInteger(final List<Integer> data) {
        return Collections.max(data);
    }

    public long maxVLong(final List<Long> data) {
        return Collections.max(data);
    }

    public IntVar maxVIntVar(final List<IntVar> data) {
        long min = Integer.MAX_VALUE;
        long max = Integer.MIN_VALUE;
        for (final IntVar v: data) {
            min = Math.min(min, v.getBuilder().getDomain(0));
            max = Math.max(max, v.getBuilder().getDomain(v.getBuilder().getDomainCount() - 1));
        }
        final IntVar ret = model.newIntVar(min, max, "");
        assert ret.getBuilder().getDomainCount() != 0;
        model.addMaxEquality(ret, data.toArray(new IntVar[0]));
        return ret;
    }

    public int minVInteger(final List<Integer> data) {
        return Collections.min(data);
    }

    public long minVLong(final List<Long> data) {
        return Collections.min(data);
    }

    public IntVar minVIntVar(final List<IntVar> data) {
        long min = Integer.MAX_VALUE;
        long max = Integer.MIN_VALUE;
        for (final IntVar v: data) {
            min = Math.min(min, v.getBuilder().getDomain(0));
            max = Math.max(max, v.getBuilder().getDomain(v.getBuilder().getDomainCount() - 1));
        }
        final IntVar ret = model.newIntVar(min, max, "");
        assert ret.getBuilder().getDomainCount() != 0;
        model.addMinEquality(ret, data.toArray(new IntVar[0]));
        return ret;
    }

    public int countV(final long[] data) {
        return data.length;
    }

    public IntVar div(final IntVar left, final int right) {
        final IntVar ret = model.newIntVar(Integer.MIN_VALUE, Integer.MAX_VALUE, "");
        model.addDivisionEquality(ret, left, model.newConstant(right));
        return ret;
    }


    public IntVar plus(final int left, final IntVar right) {
        return plus(model.newConstant(left), right);
    }

    public IntVar plus(final IntVar left, final int right) {
        final IntVar ret = model.newIntVar(Integer.MIN_VALUE, Integer.MAX_VALUE, "");
        model.addEquality(ret, LinearExpr.sum(new IntVar[]{left, model.newConstant(right)}));
        return ret;
    }

    public IntVar plus(final IntVar left, final IntVar right) {
        final IntVar ret = model.newIntVar(Integer.MIN_VALUE, Integer.MAX_VALUE, "");
        model.addEquality(ret, LinearExpr.sum(new IntVar[]{left, right}));
        return ret;
    }

    public IntVar minus(final int left, final IntVar right) {
        return minus(model.newConstant(left), right);
    }

    public IntVar minus(final IntVar left, final int right) {
        final IntVar ret = model.newIntVar(Integer.MIN_VALUE, Integer.MAX_VALUE, "");
        model.addEquality(ret, LinearExpr.sum(new IntVar[]{left, model.newConstant(-right)}));
        return ret;
    }

    public IntVar minus(final IntVar left, final IntVar right) {
        final IntVar ret = model.newIntVar(Integer.MIN_VALUE, Integer.MAX_VALUE, "");
        model.addEquality(ret, LinearExpr.scalProd(new IntVar[]{left, right}, new int[]{1, -1}));
        return ret;
    }

    public int mult(final int left, final int right) {
        return left * right;
    }

    public IntVar mult(final int left, final IntVar right) {
        return mult(right, left);
    }

    public IntVar mult(final IntVar left, final int right) {
        final long min = left.getBuilder().getDomain(0);
        final long max = left.getBuilder().getDomain(left.getBuilder().getDomainCount() - 1);
        final long lowBound = Math.min(min * right, max * right);
        final long upperBound = Math.max(min * right, max * right);
        final IntVar ret = model.newIntVar(lowBound, upperBound, "");
        assert ret.getBuilder().getDomainCount() != 0;
        model.addEquality(ret, LinearExpr.term(left, right));
        return ret;
    }

    public IntVar mult(final IntVar left, final IntVar right) {
        final IntVar ret = model.newIntVar(Integer.MIN_VALUE, Integer.MAX_VALUE, "");
        model.addProductEquality(ret, new IntVar[]{left, right});
        return ret;
    }

    public boolean eq(final boolean left, final boolean right) {
        return right == left;
    }

    public boolean eq(final String left, final String right) {
        return right.equals(left);
    }

    public boolean eq(final int left, final int right) {
        return left == right;
    }

    public boolean eq(final long left, final long right) {
        return left == right;
    }

    public IntVar eq(final String left, final IntVar right) {
        return eq(right, left);
    }

    public IntVar eq(final IntVar left, final String right) {
        return eq(left, encoder.toLong(right));
    }

    public IntVar eq(final long left, final IntVar right) {
        return eq(right, left);
    }

    public IntVar eq(final IntVar left, final long right) {
        if (getMin(left) == getMax(left) && getMin(left) == right) {
            return trueVar;
        }
        if ((right < getMin(left)) && (getMax(left) < right)) {
            return falseVar;
        }
        final IntVar bool = model.newBoolVar("");
        model.addEquality(left, right).onlyEnforceIf(bool);
        model.addDifferent(left, right).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar eq(final IntVar left, final IntVar right) {
        if (!overlaps(left, right)) {
            return falseVar;
        }
        if (fixed(left) && fixed(right)) {
            return getMin(left) == getMin(right) ? trueVar : falseVar;
        }
        final IntVar bool = model.newBoolVar("");
        model.addEquality(left, right).onlyEnforceIf(bool);
        model.addDifferent(left, right).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar eq(final IntVar left, final boolean right) {
        return eq(left, right ? 1 : 0);
    }

    public IntVar eq(final boolean left, final IntVar right) {
        return eq(right, left);
    }

    public boolean ne(final boolean left, final boolean right) {
        return right != left;
    }

    public boolean ne(final String left, final String right) {
        return !right.equals(left);
    }

    public boolean ne(final int left, final int right) {
        return left != right;
    }

    public boolean ne(final long left, final long right) {
        return left != right;
    }

    public IntVar ne(final String left, final IntVar right) {
        return ne(right, left);
    }

    public IntVar ne(final IntVar left, final String right) {
        return ne(left, encoder.toLong(right));
    }

    public IntVar ne(final long left, final IntVar right) {
        return ne(right, left);
    }

    public IntVar ne(final IntVar left, final long right) {
        final IntVar bool = model.newBoolVar("");
        model.addDifferent(left, right).onlyEnforceIf(bool);
        model.addEquality(left, right).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar ne(final IntVar left, final IntVar right) {
        final IntVar bool = model.newBoolVar("");
        model.addDifferent(left, right).onlyEnforceIf(bool);
        model.addEquality(left, right).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar ne(final boolean left, final IntVar right) {
        return ne(right, left);
    }

    public IntVar ne(final IntVar left, final boolean right) {
        return ne(left, right ? 1 : 0);
    }

    public IntVar lt(final IntVar left, final long right) {
        final IntVar bool = model.newBoolVar("");
        model.addLessThan(left, right).onlyEnforceIf(bool);
        model.addGreaterOrEqual(left, right).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar lt(final IntVar left, final IntVar right) {
        final IntVar bool = model.newBoolVar("");
        model.addLessThan(left, right).onlyEnforceIf(bool);
        model.addGreaterOrEqual(left, right).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar leq(final IntVar left, final long right) {
        if (getMax(left) <= right) {
            return trueVar;
        }
        if (getMin(left) > right) {
            return falseVar;
        }
        final IntVar bool = model.newBoolVar("");
        model.addLessOrEqual(left, right).onlyEnforceIf(bool);
        model.addGreaterThan(left, right).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar leq(final IntVar left, final IntVar right) {
        if (getMax(left) <= getMin(right)) {
            return trueVar;
        }
        if (getMin(left) > getMax(right)) {
            return falseVar;
        }
        final IntVar bool = model.newBoolVar("");
        model.addLessOrEqual(left, right).onlyEnforceIf(bool);
        model.addGreaterThan(left, right).onlyEnforceIf(bool.not());
        return bool;
    }


    public IntVar gt(final IntVar left, final long right) {
        final IntVar bool = model.newBoolVar("");
        model.addGreaterThan(left, right).onlyEnforceIf(bool);
        model.addLessOrEqual(left, right).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar gt(final IntVar left, final IntVar right) {
        final IntVar bool = model.newBoolVar("");
        model.addGreaterThan(left, right).onlyEnforceIf(bool);
        model.addLessOrEqual(left, right).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar geq(final IntVar left, final long right) {
        if (getMin(left) >= right) {
            return trueVar;
        }
        if (getMax(left) < right) {
            return falseVar;
        }
        final IntVar bool = model.newBoolVar("");
        model.addGreaterOrEqual(left, right).onlyEnforceIf(bool);
        model.addLessThan(left, right).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar geq(final IntVar left, final IntVar right) {
        if (getMin(left) >= getMax(right)) {
            return trueVar;
        }
        if (getMax(left) < getMin(right)) {
            return falseVar;
        }
        final IntVar bool = model.newBoolVar("");
        model.addGreaterOrEqual(left, right).onlyEnforceIf(bool);
        model.addLessThan(left, right).onlyEnforceIf(bool.not());
        return bool;
    }

    public boolean in(final String left, final List<String> right) {
        return right.contains(left);
    }

    public boolean in(final int left, final List<Integer> right) {
        return right.contains(left);
    }

    public boolean in(final long left, final List<Long> right) {
        return right.contains(left);
    }

    public IntVar inString(final IntVar left, final List<String> right) {
        if (right.size() == 1) {
            return eq(left, right.get(0));
        }
        if (right.size() == 0) {
            return falseVar;
        }
        final IntVar bool = model.newBoolVar("");
        final Domain domain = Domain.fromValues(right.stream().mapToLong(encoder::toLong).toArray());
        model.addLinearExpressionInDomain(left, domain).onlyEnforceIf(bool);
        model.addLinearExpressionInDomain(left, domain.complement()).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar inLong(final IntVar left, final List<Long> right) {
        if (right.size() == 1) {
            return eq(left, right.get(0));
        }
        if (right.size() == 0) {
            return falseVar;
        }
        final IntVar bool = model.newBoolVar("");
        final Domain domain = Domain.fromValues(right.stream().mapToLong(encoder::toLong).toArray());
        model.addLinearExpressionInDomain(left, domain).onlyEnforceIf(bool);
        model.addLinearExpressionInDomain(left, domain.complement()).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar inInteger(final IntVar left, final List<Integer> right) {
        if (right.size() == 1) {
            return eq(left, right.get(0));
        }
        if (right.size() == 0) {
            return falseVar;
        }

        final IntVar bool = model.newBoolVar("");
        final Domain domain = Domain.fromValues(right.stream().mapToLong(encoder::toLong).toArray());
        model.addLinearExpressionInDomain(left, domain).onlyEnforceIf(bool);
        model.addLinearExpressionInDomain(left, domain.complement()).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar inIntVar(final IntVar left, final List<IntVar> right) {
        if (right.size() == 1) {
            return eq(left, right.get(0));
        }
        if (right.size() == 0) {
            return falseVar;
        }

        final IntVar bool = model.newBoolVar("");
        final Literal[] literals = new Literal[right.size()];
        for (int i = 0; i < right.size(); i++) {
            literals[i] = eq(left, right.get(i));
        }
        model.addBoolOr(literals).onlyEnforceIf(bool);

        for (int i = 0; i < right.size(); i++) {
            literals[i] = literals[i].not();
        }
        model.addBoolAnd(literals).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar or(final boolean left, final IntVar right) {
        if (left || isTrue(right)) {
            return trueVar;
        }
        if (isFalse(right)) {
            return falseVar;
        }
        return right;
    }

    public IntVar or(final IntVar left, final boolean right) {
        return or(right, left);
    }

    public IntVar or(final IntVar left, final IntVar right) {
        if (isTrue(left) || isTrue(right)) {
            return trueVar;
        }
        if (isFalse(left) && isFalse(right)) {
            return falseVar;
        }
        final IntVar bool = model.newBoolVar("");
        model.addBoolOr(new Literal[]{left, right}).onlyEnforceIf(bool);
        model.addBoolAnd(new Literal[]{left.not(), right.not()}).onlyEnforceIf(bool.not());
        return bool;
    }


    public IntVar and(final boolean left, final IntVar right) {
        if (!left || isFalse(right)) {
            return falseVar;
        }
        if (isTrue(right)) {
            return trueVar;
        }
        return right;
    }

    public IntVar and(final IntVar left, final boolean right) {
        return and(right, left);
    }

    public IntVar and(final IntVar left, final IntVar right) {
        if (isFalse(left) || isFalse(right)) {
            return falseVar;
        }
        if (isTrue(left) && isTrue(right)) {
            return trueVar;
        }
        final IntVar bool = model.newBoolVar("");
        model.addBoolAnd(new Literal[]{left, right}).onlyEnforceIf(bool);
        model.addBoolOr(new Literal[]{left.not(), right.not()}).onlyEnforceIf(bool.not());
        return bool;
    }

    public IntVar not(final IntVar var) {
        return eq(var, 0L);
    }

    public boolean not(final boolean var) {
        return !var;
    }

    public <T> boolean allEqual(final List<T> array) {
        for (int i = 0; i < array.size() - 1; i++) {
            if (array.get(i) != array.get(i + 1)) {
                return false;
            }
        }
        return true;
    }

    public IntVar toConst(final boolean expr) {
        return expr ? trueVar : falseVar;
    }

    public IntVar toConst(final long expr) {
        return model.newConstant(expr);
    }

    public long getMax(final IntVar v) {
        return v.getBuilder().getDomain(v.getBuilder().getDomainCount() - 1);
    }

    public long getMin(final IntVar v) {
        return v.getBuilder().getDomain(0);
    }

    public boolean overlaps(final IntVar v1, final IntVar v2) {
        if ((getMax(v1) >= getMin(v2) && getMax(v1) <= getMax(v2))
                || (getMin(v1) >= getMin(v2) && getMin(v1) <= getMax(v2))) {
            return true;
        }
        return (getMax(v2) >= getMin(v1) && getMax(v2) <= getMax(v1))
                || (getMin(v2) >= getMin(v1) && getMin(v2) <= getMax(v1));
    }

    public boolean fixed(final IntVar v1) {
        return getMin(v1) == getMax(v1);
    }

    public boolean isFalse(final IntVar v) {
        return fixed(v) && getMin(v) == 0;
    }

    public boolean isTrue(final IntVar v) {
        return fixed(v) && getMin(v) == 1;
    }
}