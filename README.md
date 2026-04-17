# TypeScript Facade Generator

General-purpose TypeScript definition generator from java bytecode.

This library converts java bytecode into typescript facade, to be used by IDEs (like VSCode) for code completion.

## Adding into gradle
```groovy
repositories {
    maven {
        url "https://maven.zeith.org"
    }
}

dependencies {
    implementation("dev.zeith.libs:TsFacadeGenerator:1.0.4")
}
```

## Minimal usage example
This simple code reads the class file from file, parses the internal class model and converts it into typescript, writing it into file:
```java
byte[] bytecode = Files.readAllBytes(Path.of("Example.class"));
TypeScriptGenerator gen = new TypeScriptGenerator(ClassModel.parse(bytecode));

StringBuilder sb = new StringBuilder();
gen.generate(sb, true);

Files.writeString(Path.of("Example.ts"), sb.toString());
```


## Bulk exporting
If you want to convert more than one class, you'd want to use BulkTypeScriptExporter:
```java
File targetDir = new File("build/generated/typescript");

// Clean directory before dumping ts, otherwise the files will contain duplicates
// deleteDir(targetDir);

File input = new File("build/classes/java");

BulkTypeScriptExporter exporter = BulkTypeScriptExporter
    .builder()
    .outDir(targetDir)
    // if you want to separate into individual classes:
    .pathResolver(IPathResolver.FROM_CLASS_NAME)
    .build();

// Check every file/entry in dir/jar
IClassFileVisitor.visit(input, (name, entry) ->
{
    byte[] bytecode = entry.readAllBytes();
    ClassModel model = ClassModel.parse(bytecode);
    if(model == null || !model.name().getInternalName().contains("/") || !model.isPublic())
    {
        System.out.println("Warn: Class model skipped for " + name);
        return;
    }
    
    // If you're exporting to different files, this may be async. (Manual async implementation required)
    exporter.export(model);
});

// Optimize imports in the generated files
exporter.optimize();

// if you want to perform more than one export,
// this prevents optimize() calls from fixing imports on files that were already exported (unless the files will be exported into again)
exporter.reset();
```