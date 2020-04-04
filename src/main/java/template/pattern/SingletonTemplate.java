package template.pattern;

import com.goide.psi.GoTypeSpec;
import com.goide.refactor.template.GoTemplate;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import template.DesignPattern;

/**
 * @author DASK
 * @date 2020/3/30 2:09
 * @description //TODO 单例模式模板
 */
public class SingletonTemplate extends DesignPattern {
    private GoTypeSpec typeSpec;
    private boolean isSafe;
    private final String INSTANCE_VAR="INSTANCE_VAR";

    public SingletonTemplate(AnActionEvent event, GoTypeSpec typeSpec, boolean isSafe) {
        super(event);
        this.typeSpec = typeSpec;
        this.isSafe = isSafe;
    }

    @Override
    protected void createTemplate(@NotNull GoTemplate template) {
        if (isSafe){
            createSafeSingleton(template);
        }else {
            createUnSafeSingleton(template);
        }
    }

    @Override
    protected GoTypeSpec getGoTypeSpec() {
        return typeSpec;
    }

    private GoTemplate createUnSafeSingleton(GoTemplate template){
        String typeName=typeSpec.getName();
        /*
        *  var instance *Singleton
        * */
        template.addTextSegment("var ");
        template.addPrimaryVariable(INSTANCE_VAR,new ConstantNode("instance"));
        template.addTextSegment("*"+typeName);
        templateNewLine(template,2);
        /*
        *   func GetInstanceUnsafe() *Singleton {
        *       if instance ==nil {
        *          instance = &Singleton{}
        *       }
        *       return instance
        *   }
        * */
        template.addTextSegment("func ");
        template.addVariable("GetInstanceUnsafe", true);//为true在生成之后可以修改名称
        template.addTextSegment("() *" + typeName);
        template.addTextSegment(" {\n");
        template.addTextSegment("\tif ");
        template.addVariableSegment(INSTANCE_VAR);
        template.addTextSegment(" ==nil {\n");
        template.addVariableSegment(INSTANCE_VAR);
        template.addTextSegment(" =&"+typeName+"{}\n");
        template.addTextSegment("\t}");
        template.addTextSegment("\treturn ");
        template.addVariableSegment(INSTANCE_VAR);
        template.addEndVariable();
        template.addTextSegment("\n}");
        return template;
    }

    private GoTemplate createSafeSingleton(GoTemplate template){
        String typeName=typeSpec.getName();
        /*
         *  var instance *Singleton
         * */
        template.addTextSegment("var ");
        template.addPrimaryVariable(INSTANCE_VAR,new ConstantNode("instance"));
        template.addTextSegment("*"+typeName);
        template.addTextSegment("\n\n");
        /*
         *  var instanceOnce sync.Once
         * */
        template.addTextSegment("var ");
        template.addVariableSegment(INSTANCE_VAR);
        template.addTextSegment("Once sync.Once");
        template.addTextSegment("\n\n");
        /*
        *   func GetInstanceSafe() *Singleton {
        *       instanceOnce.Do(func() {
        *           instance = &Singleton{}
        *       })
        *       return instance
        *   }
        * */
        template.addTextSegment("func ");
        template.addVariable("GetInstanceSafe", true);//为true在生成之后可以修改名称
        template.addTextSegment("() *" + typeName);
        template.addTextSegment(" {\n");
        template.addTextSegment("\t");
        template.addVariableSegment(INSTANCE_VAR);
        template.addTextSegment("Once.Do(func() {\n");
        template.addTextSegment("\t\t");
        template.addVariableSegment(INSTANCE_VAR);
        template.addTextSegment(" = &");
        template.addTextSegment(typeName);
        template.addTextSegment("{}");
        template.addTextSegment("\t})");
        template.addTextSegment("\treturn ");
        template.addVariableSegment(INSTANCE_VAR);
        template.addEndVariable();
        template.addTextSegment("\n}");
        return template;
    }
}
