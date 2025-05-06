package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    @Pointcut("execution(* com.sky.mapper.*.*(..))&& @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}

    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) throws NoSuchMethodException {
        log.info("开始进行公共字段自动填充...");
        //获取到当前切入点的数据库操作类型
        MethodSignature signature=(MethodSignature)joinPoint.getSignature();
        AutoFill autoFill= signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType =autoFill.value();
        //获取到当前被拦截的方法的参数--实体对象
        Object[] args=joinPoint.getArgs();
        if(args==null && args.length==0){return;}
        Object entity= args[0];
        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId= BaseContext.getCurrentId();
        //根据当前不同的操作类型，为对应的属性通过反射来赋值
        if(operationType==OperationType.INSERT){
            try {
                Method setCreateUser= entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER,Long.class);
                Method setCreateTime= entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setUpdateUser= entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);
                Method setUpdateTime= entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                setCreateUser.invoke(entity,currentId);
                setCreateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);
                setUpdateTime.invoke(entity,now);
            }catch (NoSuchMethodException e){
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }else if(operationType==OperationType.UPDATE){
            try {

                Method setUpdateUser= entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class);
                Method setUpdateTime= entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);

                setUpdateUser.invoke(entity,currentId);
                setUpdateTime.invoke(entity,now);
            }catch (NoSuchMethodException e){
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
