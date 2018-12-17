/*
 *  JavaSMT is an API wrapper for a collection of SMT solvers.
 *  This file is part of JavaSMT.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.java_smt.solvers.wrapper;

import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.OptimizationProverEnvironment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.basicimpl.AbstractSolverContext;

public class WrapperSolverContext extends AbstractSolverContext {

  @Options(prefix = "solver.wrapper")
  private static class WrapperOptions {
    @Option(secure = true, description = "Which SMT solver to use as delegate.")
    public Solvers solver = Solvers.SMTINTERPOL;

    @Option(
      secure = true,
      description = "If formulas should be canonized before queried to the solver."
    )
    public boolean canonize = false;

    @Option(secure = true, description = "If answers of solvers should be cached.")
    public boolean cache = false;
  }

  private SolverContext delegate;
  private WrapperOptions options;

  public WrapperSolverContext(
      FormulaManager pFmgr, SolverContext pDelegate, WrapperOptions pOptions) {
    super(pFmgr);
    delegate = pDelegate;
    options = pOptions;
  }

  @SuppressWarnings("resource")
  public static SolverContext create(
      Configuration pConfig, SolverContextFactory pSolverContextFactory)
      throws InvalidConfigurationException {
    WrapperOptions options = new WrapperOptions();
    pConfig.inject(options);
    SolverContext delegate = pSolverContextFactory.generateContext(options.solver);
    return new WrapperSolverContext(delegate.getFormulaManager(), delegate, options);
  }

  @Override
  public String getVersion() {
    return delegate.getVersion();
  }

  @Override
  public Solvers getSolverName() {
    return delegate.getSolverName();
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  protected ProverEnvironment newProverEnvironment0(Set<ProverOptions> pOptions) {
    ProverEnvironment env = delegate.newProverEnvironment(pOptions.toArray(new ProverOptions[] {}));

    if (options.canonize) {
      // TODO: env = new CanonizingEnvironmentWrapper(env);
    }

    if (options.cache) {
      // TODO: env = new CachingEnvironmentWrapper(env);
    }

    return env;
  }

  @Override
  protected InterpolatingProverEnvironment<?> newProverEnvironmentWithInterpolation0(
      Set<ProverOptions> pSet) {
    InterpolatingProverEnvironment<?> env =
        delegate.newProverEnvironmentWithInterpolation(pSet.toArray(new ProverOptions[] {}));

    if (options.canonize) {
      // TODO: env = new CanonizingEnvironmentWrapper(env);
    }

    if (options.cache) {
      // TODO: env = new CachingEnvironmentWrapper(env);
    }

    return env;
  }

  @Override
  protected OptimizationProverEnvironment newOptimizationProverEnvironment0(
      Set<ProverOptions> pSet) {
    OptimizationProverEnvironment env =
        delegate.newOptimizationProverEnvironment(pSet.toArray(new ProverOptions[] {}));

    if (options.canonize) {
      env = new CanonizingOptimizationEnvironmentWrapper(env, delegate.getFormulaManager());
    }

    if (options.cache) {
      // TODO: env = new CachingEnvironmentWrapper(env);
    }

    return env;
  }

  @Override
  protected boolean supportsAssumptionSolving() {
    // Avoid additional wrapping
    return true;
  }
}