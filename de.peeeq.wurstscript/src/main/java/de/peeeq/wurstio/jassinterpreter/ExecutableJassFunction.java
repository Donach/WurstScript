package de.peeeq.wurstio.jassinterpreter;

import de.peeeq.wurstscript.WLogger;
import de.peeeq.wurstscript.intermediatelang.ILconst;
import de.peeeq.wurstscript.jassAst.JassFunction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public interface ExecutableJassFunction {

    ILconst execute(JassInterpreter jassInterpreter, ILconst[] arguments);


}

class UserDefinedJassFunction implements ExecutableJassFunction {

    private JassFunction jassFunction;

    public UserDefinedJassFunction(JassFunction f) {
        this.jassFunction = f;
    }

    @Override
    public ILconst execute(JassInterpreter jassInterpreter, ILconst[] arguments) {
        return jassInterpreter.executeJassFunction(jassFunction, arguments);
    }

}

class NativeJassFunction implements ExecutableJassFunction {

    private Method method;
    private ReflectionBasedNativeProvider natives;

    public NativeJassFunction(ReflectionBasedNativeProvider natives, Method method) {
        this.natives = natives;
        this.method = method;
    }

    @Override
    public ILconst execute(JassInterpreter jassInterpreter, ILconst[] arguments) {
        try {
            Object result = method.invoke(natives, (Object[]) arguments);
            return (ILconst) result;
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new Error(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            } else {
                throw new Error(e.getCause());
            }
        }
    }

}


class UnknownJassFunction implements ExecutableJassFunction {

    private String name;

    public UnknownJassFunction(String name) {
        this.name = name;
    }

    @Override
    public ILconst execute(JassInterpreter jassInterpreter, ILconst[] arguments) {
        WLogger.info("Function " + name + " could not be found.");
        throw new InterpreterException("Function " + name + " could not be found.");
    }

}

