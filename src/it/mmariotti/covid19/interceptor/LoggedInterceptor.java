package it.mmariotti.covid19.interceptor;

import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class LoggedInterceptor
{
	@AroundInvoke
	public Object intercept(InvocationContext ctx) throws Exception
	{
		Method method = ctx.getMethod();
		Object target = ctx.getTarget();
//		Object[] parameters = ctx.getParameters();

		Class<?> targetClass = target.getClass();
		Logger logger = LoggerFactory.getLogger(targetClass);

		String methodName = method.getName();

		logger.info("{}() called", methodName);

		try
		{
			return ctx.proceed();
		}
		catch(Exception e)
		{
			logger.error("{}() error: {}", methodName, e.getMessage());
			throw e;
		}
		finally
		{
			logger.info("{}() completed", methodName);
		}
	}
}
