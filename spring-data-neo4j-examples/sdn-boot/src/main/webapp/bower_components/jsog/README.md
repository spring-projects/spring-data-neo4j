# JSOG - JavaScript Object Graph

JSOG (JavaScript Object Graph) is a simple convention which allows arbitrary object graphs
to be represented in JSON. It allows a large, complicated, cyclic object graph to be seriazlied
and deserialized to and from JSON.

## The problem with JSON

JSON is widely used as a data interchange format, however, it is limited:

* Only directed acyclic graphs can be represented.
* Graphs with repeating information are duplicated on the wire and in memory.

For example:

	[
		{
			"name": "Sally",
			"secretSanta": {...Bob...}
		},
		{
			"name": "Bob",
			"secretSanta": {...Fred...}
		},
		{
			"name": "Fred",
			"secretSanta": {...Sally...}
		}
	]

This graph has cycles. Your database can represent these relationships just fine and your ORM can pull the object
graph (with references) into memory, but you cannot directly serialize to a JSON structure without stack
overflow errors.

## The JSOG solution

JSOG is a standard way to represent object graphs.

* JSOG is 100% JSON. No special parser is necessary.
* JSOG is human readable; graphs without cycles look like regular JSON.
* JSOG does not require (or interact with) pre-existing id fields.
* JSOG is fully self-describing; ids and refs are unambiguous.
* JSOG is easy to implement in any language or platform.

This is the JSOG representation of the aforementioned graph:

	[
		{
			"@id": "1",
			"name": "Sally",
			"secretSanta": {
				"@id": "2",
				"name": "Bob",
				"secretSanta": {
					"@id": "3",
					"name": "Fred",
					"secretSanta": { "@ref": "1" }
				}
			}
		},
		{ "@ref": "2" },
		{ "@ref": "3" }
	]

* @id values are arbitrary strings.
* @id definitions must come before @ref references.

### Serializing to JSOG

Each time a *new* object is encountered, give it a unique string @id. Each time a *repeated* object is encountered,
serialize as a @ref to the existing @id.

### Deserializing from JSOG

Track the @id of every object deserialized. When a @ref is encountered, replace it with the object referenced.

## Implementation

JSOG is designed to be easily implemented across platforms. Look at the existing implementations; most are
a couple dozen lines of code.

### JavaScript

[The github project which contains this README](https://github.com/jsog/jsog) includes a JavaScript
implementation of JSOG. It can be used to convert between a cyclic object graph and JSOG strings:

	string = JSOG.stringify(cyclicGraph);
	cyclicGraph = JSOG.parse(string);

Or it can be used to convert between object graphs directly:

	jsogStructure = JSOG.encode(cyclicGraph);	// has { '@ref': 'ID' } links instead of cycles
	cyclicGraph = JSOG.decode(jsogStructure);

### Other Languages

* Java: [a Jackson plugin](https://github.com/jsog/jsog-jackson)
* Python: [a Python implementation](https://github.com/jsog/jsog-python)
* Ruby: [a Ruby implementation](https://github.com/jsog/jsog-ruby)

Please contact us about other implementations so they can be linked here.

## Authors

The authors are:

* Jeff Schnitzer (jeff@infohazard.org)
* Jon Stevens (latchkey@gmail.com)

## License

This specification and software are provided under the [MIT license](http://opensource.org/licenses/MIT)
