package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;

public class ConstantPropagation extends BasicService{

  private int invocations = 0;

	public ConstantPropogation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = canonize(instance.getFullExpression(), map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}

  @Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}

  public Expression propagate(Expression expression, Map<Variable, Variable> map) {
    try {
      log.log(Level.FINEST, "Before Canonization: " + expression);
			invocations++;

      HashMap <Variable, Constant> variableMap;
      VariableVisitor varVisitor = new VariableVisitor();
      expression.accept(varVisitor);
      variableMap = varVisitor.getVars();

      PropagatingVisitor propagatingVisitor = new PropagatingVisitor(variableMap);
      expression.accept(propagatingVisitor);

      Expression propagated = propagatingVisitor.getExpression();

      log.log(Level.FINEST, "After Propagation: " + propagated);
      return propagated;

    } catch(VisitorException x) {
      log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
    }
    return null;
  }

  private static class VariableVisitor extends Visitor {

  }

  private static class PropagatingVisitor extends Visitor {

    private Stack<Expression> stack;

    //private SortedSet<IntVariable> variableSet;
    private HashMap <Variable, Constant> varMap;

    //private boolean unsatisfiable;

		//private boolean linearInteger;

    public PropagatingVisitor(HashMap <Variable, Constant> varMap) {
      stack = new Stack<Expression>();
      this.variableSet = varMap;
      //unsatisfiable = false;
      //linearInteger = true;
    }

    public Expression getExpression() {
			return stack.pop();
		}

    @Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
		}

    @Override
		public void postVisit(Variable variable) {
			if (linearInteger && !unsatisfiable) {
				if (variable instanceof IntVariable) {
					variableSet.add((IntVariable) variable);
					stack.push(new Operation(Operation.Operator.MUL, Operation.ONE,
							variable));
				} else {
					stack.clear();
					linearInteger = false;
				}
			}
		}

    @Override
    public void postVisit(Operation operation) throws VisitorException {
      stack.push(operation);
    }



  }
}
