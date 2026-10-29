package dev.raze.rzvault;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Baseline was captured from the original VaultAPI 1.7 binary, not rebuilt from this source. */
class ApiCompatibilityTest {
    @Test void classicPublicAndProtectedApiMatchesTheOriginalBinary() throws Exception {
        Set<String> expected;
        try(var reader=new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/vault-api-1.7-abi.txt"),StandardCharsets.UTF_8))) {
            expected=reader.lines().collect(Collectors.toCollection(TreeSet::new));
        }
        Set<String> actual=new TreeSet<>();
        for(String row:expected) {
            if(!row.startsWith("C|"))continue;
            String name=row.split("\\|",-1)[1];
            Class<?> type=Class.forName(name.replace('/','.'),false,getClass().getClassLoader());
            String parent=type.getSuperclass()==null?"java/lang/Object":internal(type.getSuperclass());
            String interfaces=Arrays.stream(type.getInterfaces()).map(ApiCompatibilityTest::internal).sorted().collect(Collectors.joining(","));
            actual.add("C|"+name+"|"+parent+"|"+interfaces+"|"+flags(type.getModifiers()&(Modifier.PUBLIC|Modifier.FINAL|Modifier.ABSTRACT)));
            for(var field:type.getDeclaredFields())if(visible(field.getModifiers()))
                actual.add("F|"+name+"|"+field.getName()+"|"+field.getType().descriptorString()+"|"+flags(field.getModifiers()));
            for(var method:type.getDeclaredMethods())if(visible(method.getModifiers()))
                actual.add("M|"+name+"|"+method.getName()+"|"+descriptor(method.getParameterTypes(),method.getReturnType())+"|"+flags(method.getModifiers()));
            for(var constructor:type.getDeclaredConstructors())if(visible(constructor.getModifiers()))
                actual.add("M|"+name+"|<init>|"+descriptor(constructor.getParameterTypes(),void.class)+"|"+flags(constructor.getModifiers()));
        }
        assertEquals(6,expected.stream().filter(v->v.startsWith("C|")).count());
        assertEquals(expected,actual);
    }
    private static boolean visible(int flags){return Modifier.isPublic(flags)||Modifier.isProtected(flags);}
    private static String internal(Class<?> type){return type.getName().replace('.','/');}
    private static String descriptor(Class<?>[] parameters,Class<?> result){return "("+Arrays.stream(parameters).map(Class::descriptorString).collect(Collectors.joining())+")"+result.descriptorString();}
    private static String flags(int flags){
        var names=new java.util.ArrayList<String>();
        if(Modifier.isPublic(flags))names.add("public");if(Modifier.isProtected(flags))names.add("protected");
        if(Modifier.isStatic(flags))names.add("static");if(Modifier.isFinal(flags))names.add("final");if(Modifier.isAbstract(flags))names.add("abstract");
        return String.join(",",names);
    }
}
