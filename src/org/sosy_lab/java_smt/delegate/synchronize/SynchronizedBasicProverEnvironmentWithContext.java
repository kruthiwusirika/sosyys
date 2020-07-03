/*
 *  JavaSMT is an API wrapper for a collection of SMT solvers.
 *  This file is part of JavaSMT.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.java_smt.delegate.synchronize;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverException;

class SynchronizedBasicProverEnvironmentWithContext<T> implements BasicProverEnvironment<T> {

  private final BasicProverEnvironment<T> delegate;
  final FormulaManager manager;
  final FormulaManager otherManager;
  final SolverContext sync;

  SynchronizedBasicProverEnvironmentWithContext(
      BasicProverEnvironment<T> pDelegate,
      SolverContext pSync,
      FormulaManager pManager,
      FormulaManager pOtherManager) {
    delegate = checkNotNull(pDelegate);
    sync = checkNotNull(pSync);
    manager = checkNotNull(pManager);
    otherManager = checkNotNull(pOtherManager);
  }

  List<BooleanFormula> translate(
      Collection<BooleanFormula> fs, FormulaManager from, FormulaManager to) {
    ImmutableList.Builder<BooleanFormula> result = ImmutableList.builder();
    synchronized (sync) {
      for (BooleanFormula f : fs) {
        result.add(to.translateFrom(f, from));
      }
    }
    return result.build();
  }

  @Override
  public void pop() {
    delegate.pop();
  }

  @Override
  public @Nullable T addConstraint(BooleanFormula pConstraint) throws InterruptedException {
    BooleanFormula constraint;
    synchronized (sync) {
      constraint = otherManager.translateFrom(pConstraint, manager);
    }
    return delegate.addConstraint(constraint);
  }

  @Override
  public void push() {
    delegate.push();
  }

  @Override
  public boolean isUnsat() throws SolverException, InterruptedException {
    return delegate.isUnsat();
  }

  @Override
  public boolean isUnsatWithAssumptions(Collection<BooleanFormula> pAssumptions)
      throws SolverException, InterruptedException {
    return delegate.isUnsatWithAssumptions(translate(pAssumptions, manager, otherManager));
  }

  @SuppressWarnings("resource")
  @Override
  public Model getModel() throws SolverException {
    synchronized (sync) {
      return new SynchronizedModelWithContext(delegate.getModel(), sync, manager, otherManager);
    }
  }

  @Override
  public List<BooleanFormula> getUnsatCore() {
    return translate(delegate.getUnsatCore(), otherManager, manager);
  }

  @Override
  public Optional<List<BooleanFormula>> unsatCoreOverAssumptions(
      Collection<BooleanFormula> pAssumptions) throws SolverException, InterruptedException {
    Optional<List<BooleanFormula>> core =
        delegate.unsatCoreOverAssumptions(translate(pAssumptions, manager, otherManager));
    if (core.isPresent()) {
      return Optional.of(translate(core.orElseThrow(), otherManager, manager));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public void close() {
    synchronized (sync) {
      delegate.close();
    }
  }

  @Override
  public <R> R allSat(AllSatCallback<R> pCallback, List<BooleanFormula> pImportant)
      throws InterruptedException, SolverException {
    AllSatCallback<R> callback = new AllSatCallbackWithContext<>(pCallback);
    synchronized (sync) {
      return delegate.allSat(callback, translate(pImportant, manager, otherManager));
    }
  }

  private class AllSatCallbackWithContext<R> implements AllSatCallback<R> {

    private final AllSatCallback<R> delegateCallback;

    AllSatCallbackWithContext(AllSatCallback<R> pDelegateCallback) {
      delegateCallback = checkNotNull(pDelegateCallback);
    }

    @Override
    public void apply(List<BooleanFormula> pModel) {
      delegateCallback.apply(translate(pModel, otherManager, manager));
    }

    @Override
    public R getResult() throws InterruptedException {
      return delegateCallback.getResult();
    }
  }
}