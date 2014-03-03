package org.fluxtream.aspects;

import java.lang.reflect.Method;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import com.newrelic.api.agent.NewRelic;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * User: candide
 * Date: 12/03/13
 * Time: 11:03
 */
@Aspect
public class NewRelicTransactionAspect {

    FlxLogger logger = FlxLogger.getLogger(NewRelicTransactionAspect.class);

    @Around("@annotation(get)")
    public Object logGETRESTCall(ProceedingJoinPoint pjp, final GET get) throws Throwable {
        return logRESTCall(pjp, "GET");
    }

    @Around("@annotation(post)")
    public Object logPOSTRESTCall(ProceedingJoinPoint pjp, final POST post) throws Throwable {
        return logRESTCall(pjp, "POST");
    }

    @Around("@annotation(delete)")
    public Object logDELETERESTCall(ProceedingJoinPoint pjp, final DELETE delete) throws Throwable {
        return logRESTCall(pjp, "DELETE");
    }

    private Object logRESTCall(ProceedingJoinPoint pjp, String httpMethod) throws Throwable {
        Object obj;
        logArguments(pjp, httpMethod);
        try {
            obj = pjp.proceed();
        } catch (Exception e) {
            NewRelic.noticeError(e);
            throw e;
        }
        return obj;
    }

    private void logArguments(ProceedingJoinPoint pjp, String httpMethod) {
        try {
            StringBuilder sb = new StringBuilder(httpMethod).append(" ");
            final Path classPathAnnotation = pjp.getTarget().getClass().getAnnotation(Path.class);
            if (classPathAnnotation!=null)
                sb.append(classPathAnnotation.value());
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            final Path methodPathAnnotation = method.getAnnotation(Path.class);
            if (methodPathAnnotation!=null)
                sb.append(methodPathAnnotation.value());
            NewRelic.setTransactionName(null, sb.toString());
        } catch (Throwable t) {
            logger.warn("Could not log arguments for join point: " + pjp);
        }
    }

}
