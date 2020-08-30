package com.plx.android.compiler.processer;

import com.google.auto.service.AutoService;
import com.plx.android.compiler.utils.Logger;
import com.plx.android.compiler.utils.TypeUtils;
import com.plx.android.wrouter.facade.annotation.Autowired;
import com.plx.android.wrouter.facade.annotation.Route;
import com.plx.android.wrouter.facade.enums.RouteType;
import com.plx.android.wrouter.facade.model.RouteMeta;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static com.plx.android.compiler.utils.Consts.ACTIVITY;
import static com.plx.android.compiler.utils.Consts.ANNOTATION_TYPE_AUTOWIRED;
import static com.plx.android.compiler.utils.Consts.ANNOTATION_TYPE_ROUTE;
import static com.plx.android.compiler.utils.Consts.FRAGMENT;
import static com.plx.android.compiler.utils.Consts.FRAGMENT_V4;
import static com.plx.android.compiler.utils.Consts.IPROVIDER;
import static com.plx.android.compiler.utils.Consts.IPROVIDER_GROUP;
import static com.plx.android.compiler.utils.Consts.IROUTE_GROUP;
import static com.plx.android.compiler.utils.Consts.ITROUTE_ROOT;
import static com.plx.android.compiler.utils.Consts.KEY_GENERATE_DOC_NAME;
import static com.plx.android.compiler.utils.Consts.KEY_MODULE_NAME;
import static com.plx.android.compiler.utils.Consts.METHOD_LOAD_INTO;
import static com.plx.android.compiler.utils.Consts.NAME_OF_GROUP;
import static com.plx.android.compiler.utils.Consts.NAME_OF_PROVIDER;
import static com.plx.android.compiler.utils.Consts.NAME_OF_ROOT;
import static com.plx.android.compiler.utils.Consts.NO_MODULE_NAME_TIPS;
import static com.plx.android.compiler.utils.Consts.PACKAGE_OF_GENERATE_FILE;
import static com.plx.android.compiler.utils.Consts.SEPARATOR;
import static com.plx.android.compiler.utils.Consts.SERVICE;
import static com.plx.android.compiler.utils.Consts.TAG;
import static com.plx.android.compiler.utils.Consts.VALUE_ENABLE;
import static com.plx.android.compiler.utils.Consts.WARNING_TIPS;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Created by plx on 19/4/20.
 */
@AutoService(Processor.class)
@SupportedOptions({KEY_MODULE_NAME, KEY_GENERATE_DOC_NAME})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({ANNOTATION_TYPE_ROUTE, ANNOTATION_TYPE_AUTOWIRED})
public class RouteProcessor extends AbstractProcessor {

    private Map<String, Set<RouteMeta>> groupMap = new HashMap<>(); // ModuleName and routeMeta.
    private Map<String, String> rootMap = new TreeMap<>();  // Map of root metas, used for generate class file in order.
    private Filer mFiler;       // File util, write class file into disk.
    private Logger logger;
    private Types types;
    private Elements elements;
    private TypeUtils typeUtils;
    private String moduleName = null;   // Module name, maybe its 'app' or others
    private TypeMirror iProvider = null;

    private boolean generateDoc;    // If need generate router doc
    private Writer docWriter;       // Writer used for write doc


    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFiler = processingEnv.getFiler();                  // Generate class.
        types = processingEnv.getTypeUtils();            // Get type utils.
        elements = processingEnv.getElementUtils();      // Get class meta.

        typeUtils = new TypeUtils(types, elements);
        logger = new Logger(processingEnv.getMessager());   // Package the log utils.

        // Attempt to get user configuration [moduleName]
        Map<String, String> options = processingEnv.getOptions();
        if (MapUtils.isNotEmpty(options)) {
            moduleName = options.get(KEY_MODULE_NAME);
            generateDoc = VALUE_ENABLE.equals(options.get(KEY_GENERATE_DOC_NAME));
        }

        if (StringUtils.isNotEmpty(moduleName)) {
            moduleName = moduleName.replaceAll("[^0-9a-zA-Z_]+", "");

            logger.info("The user has configuration the module name, it was [" + moduleName + "]");
        } else {
            logger.error(NO_MODULE_NAME_TIPS);
            throw new RuntimeException(TAG + "Compiler >>> No module name, for more information, look at gradle log.");
        }

        if (elements.getTypeElement(IPROVIDER) == null){
            throw new RuntimeException(TAG + "Compiler >>> No class " + IPROVIDER + " found");
        }
        iProvider = elements.getTypeElement(IPROVIDER).asType();

        logger.info(">>> RouteProcessor init. <<<");
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (CollectionUtils.isNotEmpty(set)) {
            Set<? extends Element> routeElements = roundEnvironment.getElementsAnnotatedWith(Route.class);
            try {
                logger.info(">>> Found routes, start... <<<");
                parseRoutes(routeElements);

            } catch (Exception e) {
                logger.error(e);
            }
            return true;
        }
        return false;
    }

    private void parseRoutes(Set<? extends Element> routeElements) throws IOException{
        if (CollectionUtils.isEmpty(routeElements)){
            return;
        }
        // prepare the type an so on.

        logger.info(">>> Found routes, size is " + routeElements.size() + " <<<");

        rootMap.clear();

        TypeMirror type_Activity = elements.getTypeElement(ACTIVITY).asType();
        TypeMirror type_Service = elements.getTypeElement(SERVICE).asType();
        TypeMirror fragmentTm = elements.getTypeElement(FRAGMENT).asType();
        TypeMirror fragmentTmV4 = elements.getTypeElement(FRAGMENT_V4).asType();

        // Interface of WRouter
        TypeElement type_IRouteGroup = elements.getTypeElement(IROUTE_GROUP);
        TypeElement type_IProviderGroup = elements.getTypeElement(IPROVIDER_GROUP);
        ClassName routeMetaCn = ClassName.get(RouteMeta.class);
        ClassName routeTypeCn = ClassName.get(RouteType.class);

        /*
           Build input type, format as :

           ```Map<String, Class<? extends IRouteGroup>>```
         */
        ParameterizedTypeName inputMapTypeOfRoot = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(
                        ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(type_IRouteGroup))
                )
        );

        /*

          ```Map<String, RouteMeta>```
         */
        ParameterizedTypeName inputMapTypeOfGroup = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(RouteMeta.class)
        );

        /*
          Build input param name.
         */
        ParameterSpec rootParamSpec = ParameterSpec.builder(inputMapTypeOfRoot, "routes").build();
        ParameterSpec groupParamSpec = ParameterSpec.builder(inputMapTypeOfGroup, "atlas").build();
        ParameterSpec providerParamSpec = ParameterSpec.builder(inputMapTypeOfGroup, "providers").build();  // Ps. its param type same as groupParamSpec!

        /*
          Build method : 'loadInto'
         */
        MethodSpec.Builder loadIntoMethodOfRootBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(rootParamSpec);

        //  Follow a sequence, find out metas of group first, generate java file, then statistics them as root.
        for (Element element : routeElements) {
            TypeMirror tm = element.asType();
            Route route = element.getAnnotation(Route.class);
            RouteMeta routeMeta;
            if (types.isSubtype(tm, type_Activity)) {                 // Activity
                logger.info(">>> Found activity route: " + tm.toString() + " <<<");

                // Get all fields annotation by @Autowired
                Map<String, Integer> paramsType = new HashMap<>();
                Map<String, Autowired> injectConfig = new HashMap<>();
                for (Element field : element.getEnclosedElements()) {
                    if (field.getKind().isField() && field.getAnnotation(Autowired.class) != null && !types.isSubtype(field.asType(), iProvider)) {
                        // It must be field, then it has annotation, but it not be provider.
                        Autowired paramConfig = field.getAnnotation(Autowired.class);
                        String injectName = StringUtils.isEmpty(paramConfig.name()) ? field.getSimpleName().toString() : paramConfig.name();
                        paramsType.put(injectName, typeUtils.typeExchange(field));
                        injectConfig.put(injectName, paramConfig);
                    }
                }
//                routeMeta = new RouteMeta(route, element, RouteType.ACTIVITY, paramsType);
//                routeMeta.setInjectConfig(injectConfig);
                for (String path : route.paths()){
                    routeMeta = new RouteMeta(RouteType.ACTIVITY, element, null, route.name(), path, route.group(), paramsType, route.priority(), route.extras());
                    routeMeta.setInjectConfig(injectConfig);
                    categories(routeMeta);
                }
            }else if (types.isSubtype(tm, iProvider)) {         // IProvider
                logger.info(">>> Found provider route: " + tm.toString() + " <<<");
//                routeMeta = new RouteMeta(route, element, RouteType.PROVIDER, null);
                for (String path : route.paths()) {
                    routeMeta = new RouteMeta(RouteType.PROVIDER, element, null, route.name(), path, route.group(), null, route.priority(), route.extras());
                    categories(routeMeta);
                }
            } else if (types.isSubtype(tm, type_Service)) {           // Service
                logger.info(">>> Found service route: " + tm.toString() + " <<<");
//                routeMeta = new RouteMeta(route, element, RouteType.parse(SERVICE), null);
                for (String path : route.paths()) {
                    routeMeta = new RouteMeta(RouteType.parse(SERVICE), element, null, route.name(), path, route.group(), null, route.priority(), route.extras());
                    categories(routeMeta);
                }

            } else if (types.isSubtype(tm, fragmentTm) || types.isSubtype(tm, fragmentTmV4)) {
                logger.info(">>> Found fragment route: " + tm.toString() + " <<<");
//                routeMeta = new RouteMeta(route, element, RouteType.parse(FRAGMENT), null);
                for (String path : route.paths()) {
                    routeMeta = new RouteMeta(RouteType.parse(FRAGMENT), element, null, route.name(), path, route.group(), null, route.priority(), route.extras());
                    categories(routeMeta);
                }
            } else {
                throw new RuntimeException(TAG + "Compiler >>> Found unsupported class type, type = [" + types.toString() + "].");
            }

        }

        MethodSpec.Builder loadIntoMethodOfProviderBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(providerParamSpec);

//        Map<String, List<RouteDoc>> docSource = new HashMap<>();
        // Start generate java source, structure is divided into upper and lower levels, used for demand initialization.
        for (Map.Entry<String, Set<RouteMeta>> entry : groupMap.entrySet()) {
            String groupName = entry.getKey();

            MethodSpec.Builder loadIntoMethodOfGroupBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(groupParamSpec);

//            List<RouteDoc> routeDocList = new ArrayList<>();

            // Build group method body
            Set<RouteMeta> groupData = entry.getValue();
            for (RouteMeta routeMeta : groupData) {
//                RouteDoc routeDoc = extractDocInfo(routeMeta);

                ClassName className = ClassName.get((TypeElement) routeMeta.getRawType());

                switch (routeMeta.getType()) {
                    case PROVIDER:  // Need cache provider's super class
                        List<? extends TypeMirror> interfaces = ((TypeElement) routeMeta.getRawType()).getInterfaces();
                        for (TypeMirror tm : interfaces) {
//                            routeDoc.addPrototype(tm.toString());

                            if (types.isSameType(tm, iProvider)) {   // Its implements iProvider interface himself.
                                // This interface extend the IProvider, so it can be used for mark provider
                                loadIntoMethodOfProviderBuilder.addStatement(
                                        "providers.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, null, " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + "))",
                                        (routeMeta.getRawType()).toString(),
                                        routeMetaCn,
                                        routeTypeCn,
                                        className,
                                        routeMeta.getPath(),
                                        routeMeta.getGroup());
                            } else if (types.isSubtype(tm, iProvider)) {
                                // This interface extend the IProvider, so it can be used for mark provider
                                loadIntoMethodOfProviderBuilder.addStatement(
                                        "providers.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, null, " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + "))",
                                        tm.toString(),    // So stupid, will duplicate only save class name.
                                        routeMetaCn,
                                        routeTypeCn,
                                        className,
                                        routeMeta.getPath(),
                                        routeMeta.getGroup());
                            }
                        }
                        break;
                    default:
                        break;
                }

                // Make map body for paramsType
                StringBuilder mapBodyBuilder = new StringBuilder();
                Map<String, Integer> paramsType = routeMeta.getParamsType();
                Map<String, Autowired> injectConfigs = routeMeta.getInjectConfig();
                if (MapUtils.isNotEmpty(paramsType)) {
//                    List<RouteDoc.Param> paramList = new ArrayList<>();
                    for (Map.Entry<String, Integer> types : paramsType.entrySet()) {
                        mapBodyBuilder.append("put(\"").append(types.getKey()).append("\", ").append(types.getValue()).append("); ");
//
//                        RouteDoc.Param param = new RouteDoc.Param();
//                        Autowired injectConfig = injectConfigs.get(types.getKey());
//                        param.setKey(types.getKey());
//                        param.setType(TypeKind.values()[types.getValue()].name().toLowerCase());
//                        param.setDescription(injectConfig.desc());
//                        param.setRequired(injectConfig.required());
//
//                        paramList.add(param);
                    }
//
//                    routeDoc.setParams(paramList);
                }

                String mapBody = mapBodyBuilder.toString();

                loadIntoMethodOfGroupBuilder.addStatement(
                        "atlas.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, " + (StringUtils.isEmpty(mapBody) ? null : ("new java.util.HashMap<String, Integer>(){{" + mapBodyBuilder.toString() + "}}")) + ", " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + "))",
                        routeMeta.getPath(),
                        routeMetaCn,
                        routeTypeCn,
                        className,
                        routeMeta.getPath().toLowerCase(),
                        routeMeta.getGroup().toLowerCase());

//                routeDoc.setClassName(className.toString());
//                routeDocList.add(routeDoc);
            }

            // Generate groups
            String groupFileName = NAME_OF_GROUP + groupName;
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(groupFileName)
                            .addJavadoc(WARNING_TIPS)
                            .addSuperinterface(ClassName.get(type_IRouteGroup))
                            .addModifiers(PUBLIC)
                            .addMethod(loadIntoMethodOfGroupBuilder.build())
                            .build()
            ).build().writeTo(mFiler);

            logger.info(">>> Generated group: " + groupName + "<<<");
            rootMap.put(groupName, groupFileName);
//            docSource.put(groupName, routeDocList);
        }

        if (MapUtils.isNotEmpty(rootMap)) {
            // Generate root meta by group name, it must be generated before root, then I can find out the class of group.
            for (Map.Entry<String, String> entry : rootMap.entrySet()) {
                loadIntoMethodOfRootBuilder.addStatement("routes.put($S, $T.class)", entry.getKey(), ClassName.get(PACKAGE_OF_GENERATE_FILE, entry.getValue()));
            }
        }

        // Output route doc
//        if (generateDoc) {
//            docWriter.append(JSON.toJSONString(docSource, SerializerFeature.PrettyFormat));
//            docWriter.flush();
//            docWriter.close();
//        }

        // Write provider into disk
        String providerMapFileName = NAME_OF_PROVIDER + SEPARATOR + moduleName;
        JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                TypeSpec.classBuilder(providerMapFileName)
                        .addJavadoc(WARNING_TIPS)
                        .addSuperinterface(ClassName.get(type_IProviderGroup))
                        .addModifiers(PUBLIC)
                        .addMethod(loadIntoMethodOfProviderBuilder.build())
                        .build()
        ).build().writeTo(mFiler);

        logger.info(">>> Generated provider map, name is " + providerMapFileName + " <<<");

        // Write root meta into disk.
        String rootFileName = NAME_OF_ROOT + SEPARATOR + moduleName;
        JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                TypeSpec.classBuilder(rootFileName)
                        .addJavadoc(WARNING_TIPS)
                        .addSuperinterface(ClassName.get(elements.getTypeElement(ITROUTE_ROOT)))
                        .addModifiers(PUBLIC)
                        .addMethod(loadIntoMethodOfRootBuilder.build())
                        .build()
        ).build().writeTo(mFiler);

        logger.info(">>> Generated root, name is " + rootFileName + " <<<");
    }

    /**
     * Sort metas in group.
     *
     * @param routeMete metas.
     */
    private void categories(RouteMeta routeMete) {
        if (routeVerify(routeMete)) {
            logger.info(">>> Start categories, group = " + routeMete.getGroup() + ", path = " + routeMete.getPath() + " <<<");
            Set<RouteMeta> routeMetas = groupMap.get(routeMete.getGroup());
            if (CollectionUtils.isEmpty(routeMetas)) {
                Set<RouteMeta> routeMetaSet = new TreeSet<>(new Comparator<RouteMeta>() {
                    @Override
                    public int compare(RouteMeta r1, RouteMeta r2) {
                        try {
                            return r1.getPath().compareTo(r2.getPath());
                        } catch (NullPointerException npe) {
                            logger.error(npe.getMessage());
                            return 0;
                        }
                    }
                });
                routeMetaSet.add(routeMete);
                groupMap.put(routeMete.getGroup(), routeMetaSet);
            } else {
                routeMetas.add(routeMete);
            }
        } else {
            logger.warning(">>> Route meta verify error, group is " + routeMete.getGroup() + " <<<");
        }
    }

    /**
     * Verify the route meta
     *
     * @param meta raw meta
     */
    private boolean routeVerify(RouteMeta meta) {
        String path = meta.getPath();

        if (StringUtils.isEmpty(path) || !path.startsWith("/")) {   // The path must be start with '/' and not empty!
            return false;
        }

        if (StringUtils.isEmpty(meta.getGroup())) { // Use default group(the first word in path)
            try {
                String defaultGroup = path.substring(1, path.indexOf("/", 1));
                if (StringUtils.isEmpty(defaultGroup)) {
                    return false;
                }

                meta.setGroup(defaultGroup);
                return true;
            } catch (Exception e) {
                logger.error("Failed to extract default group! " + e.getMessage());
                return false;
            }
        }

        return true;
    }
}
