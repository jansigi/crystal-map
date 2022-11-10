[![](https://jitpack.io/v/SchwarzIT/andcouchbaseentity.svg)](https://jitpack.io/#SchwarzIT/andcouchbaseentity)
[![Build Status](https://github.com/SchwarzIT/andcouchbaseentity/actions/workflows/android_pre_hook.yml/badge.svg)
[![codecov](https://codecov.io/gh/SchwarzIT/andcouchbaseentity/branch/master/graph/badge.svg)](https://codecov.io/gh/SchwarzIT/andcouchbaseentity)


A Framework for all Mapheads out there

## What is this Framework about?

* generate pojos for easy map interactions (@MapWrapper)
* entity framework for all databases which represents it's data in maps (@Entity)
* serialise/deserialise objects of complex classes in maps (@Mapper)


## Quick View

### @Entity / @MapWrapper

```kotlin
@Entity(database = "mydb_db")
@Fields(
        Field(name = "type", type = String::class, defaultValue = "product", readonly = true),
        Field(name = "name", type = String::class),
        Field(name = "comments", type = UserComment::class, list = true),
        Field(name = "image", type = Blob::class),
        Field(name = "identifiers", type = String::class, list = true)
)
@Queries(
        Query(fields = ["type"])
)
open class Product
```

```kotlin
@MapWrapper
@Fields(
        Field(name = "comment", type = String::class),
        Field(name = "user", type = String::class, defaultValue = "anonymous"),
        Field("age", type = Integer::class, defaultValue = "0")
)
open class UserComment
```
#### Result

```kotlin
        ProductEntity
                .create()
                .builder()
                .setName("Beer")
                .setComments(listOf(UserCommentWrapper
                        .create()
                        .builder()
                        .setComment("very awesome")
                        .exit()))
                .setImage(Blob("image/jpeg", resources.openRawResource(R.raw.ic_kaufland_placeholder)))
                .exit()
                .save()

        val allEntitiesOfType = ProductEntity.findByType()
        val resultOfAComplexQuery = ProductEntity.someComplexQuery("foo")
```

### @Mapper

```kotlin
@Mapper
class MyViewModel : ViewModel() {

    @Mapify
    var innerObjectList: List<MyMapifyableTest> = listOf(MyMapifyableTest(simple))

    @Mapify
    var innerObjectMap: Map<String, MyMapifyableTest> = mapOf("test" to MyMapifyableTest(simple))

    @Mapify
    var testSerializable: TestSerializable = TestSerializable(simple, 5)

    @Mapify(nullableIndexes = [0])
    var product: ProductEntity? = null

    @Mapify
    var booleanValue: Boolean = true

    @Mapify(nullableIndexes = [0])
    var bigDecimalValue: BigDecimal? = null
}
```
#### Result

 ``` kotlin

   // This is generated by the Annotation Processor
   val mapper = MyViewModelMapper()
   val oldObj = ViewModelProvider(this).get(MyViewModel::class.java)

   // Save obj to Map
   val mapToPersist = mapper.toMap(oldObj)

   val newObj = ViewModelProvider(this).get(MyViewModel::class.java)
   // restore obj
   mapper.fromMap(newObj, mapToPersist)

 ```



## Implementation

### [**Setup**](https://github.com/SchwarzIT/andcouchbaseentity/wiki/%5B1%5D-Setup)

### [**Entity & MapWrapper**](https://github.com/SchwarzIT/andcouchbaseentity/wiki/%5B2%5D-Entity---MapWrapper)

### [**Mapper**](https://github.com/SchwarzIT/andcouchbaseentity/wiki/%5B3%5D-Mapper)

### [**Cookbook (more useful features with examples)**](https://github.com/SchwarzIT/andcouchbaseentity/wiki/%5B5%5D-Cookbook)

### [**Migration (from old legacy version of andcouchbasentity)**](https://github.com/SchwarzIT/andcouchbaseentity/wiki/%5B5%5D-Cookbook)
  
