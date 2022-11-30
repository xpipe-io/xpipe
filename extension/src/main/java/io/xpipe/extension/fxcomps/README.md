# FxComps - Compound Components for JavaFX

The FxComps library provides a new approach to creating JavaFX interfaces and
offers a quicker and more robust user interface development workflow.
This library is compatible and can be used with any other JavaFX library.

## Principles

#### A comp is a Node/Region factory, not just another fancy wrapper for existing classes

It is advantageous to define a certain component to be a factory
that can create an instances of a JavaFX Node each time it is called.
By using this factory architecture, the scene contents can
be rebuilt entirely by invoking the root component factory.
See the [hot reload](#Hot-Reload) section on how this can be used.

Of course, if a component is a compound component that has children,
the parent factory has to incorporate the child factories into its creation process.
This can be done in fxcomps.

#### A comp should produce a transparent representation of Regions and Controls

In JavaFX, using skins allows for flexibility when generating the look and feel for a control.
One limitation of this approach is that the generated node tree is not very transparent
for developers who are especially interested in styling it.
This is caused by the fact that a skin does not expose the information required to style
it completely or even alter it without creating a new Skin class.

A comp should be designed to allow developers to easily expose as much information
about the produced node tree structure using the CompStructure class.
In case you don't want to expose the detailed structure of your comp,
you can also just use a very simple structure.

#### A comp should produce a Region instead of a Node

In practice, working with the very abstract node class comes with its fair share of limitations.
It is much easier to work with region instances, as they have various width and height properties.
Since pretty much every Node is also a Region, the main focus of comps are regions.
In case you are dealing with Nodes that are not Regions, like an ImageView or WebView,
you can still wrap them inside for example a StackPane to obtain a Region again that you can work with.

#### The generation process of a comp can be augmented

As comps are factories, any changes that should be applied to all produced
Node instances must be integrated into the factory pipeline.
This can be achieved with the Augment class, which allows you
to alter the produced node after the base factory has finished.

#### Properties used by Comps should be managed by the user, not the Comp itself

This allows Comps to only be a thin wrapper around already existing
Observables/Properties and gives the user the ability to complete control the handling of Properties.
This approach is also required for the next point.

#### A comp should not break when used Observables are updated from a thread that is not the platform thread

One common limitation of using JavaFX is that many things break when
calling any method from another thread that is not the platform thread.
While in many cases these issues can be mitigated by wrapping a problematic call in a Platform.runLater(...),
some problematic instances are harder to fix, for example Observable bindings.
In JavaFX, there is currently no way to propagate changes of an Observable
to other bound Observables only using the platform thread, when the original change was made from a different thread.
The FxComps library provides a solution with the PlatformThread.sync(...) methods and strongly encourages that
Comps make use of these methods in combination with user-managed properties
to allow for value changes for Observables from any thread without issue.

## Hot reload

The reason a Comp is designed to be a factory is to allow for hot
reloading your created GUI in conjunction with the hot-reload functionality in your IDE:

````java
    void setupReload(Scene scene, Comp<?> content) {
        var contentR = content.createRegion();
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode().equals(KeyCode.F5)) {
                var newContentR = content.createRegion();
                scene.setRoot(newContentR);
                event.consume();
            }
        });
    }
````

If you for example bind your IDE Hot Reload to F4 and your Scene reload listener to F5,
you can almost instantly apply any changes made to your GUI code without restarting.
You can also implement a similar solution to also reload your stylesheets and translations.

## Library contents

Aside from the base classes needed to implement the principles listed above,
this library also comes with a few very basic Comp implementations and some Augments.
These are very general implementations and can be seen as example implementations.

#### Comps

- [HorizontalComp](src/main/java/io/xpipe/fxcomps/comp/HorizontalComp.java) /
  [VerticalComp](src/main/java/io/xpipe/fxcomps/comp/VerticalComp.java): Simple Comp implementation to create a
  HBox/VBox using Comps as input
- [StackComp](src/main/java/io/xpipe/fxcomps/comp/StackComp.java): Simple Comp implementation to easily create a stack
  pane using Comps as input
- [StackComp](src/main/java/io/xpipe/fxcomps/comp/LabelComp.java): Simple Comp implementation for a label

#### Augments

- [GrowAugment](src/main/java/io/xpipe/fxcomps/augment/GrowAugment.java): Binds the width/height of a Comp to its
  parent, adjusted for parent padding
- [PopupMenuComp](src/main/java/io/xpipe/fxcomps/augment/PopupMenuAugment.java): Allows you to show a context menu when
  a comp is left-clicked in addition to right-click

## Creating a basic comp

As the central idea of this library is that you create your own Comps, it is designed to be very simple:

````java
        var b = Comp.of(() -> new Button("Button"));
        var l = Comp.of(() -> new Label("Label"));
        
        // Create an HBox factory and apply some Augments to it
        var layoutFactory = new HorizontalComp(List.of(b, l))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER))
                .apply(GrowAugment.create(true, true))
                .styleClass("layout");
        
        // You can now create node instances of your layout
        var region = layoutFactory.createRegion();
````

Most simple Comp definitions can be defined inline with the `Comp.of(...)` method.

## Creating more complex comps

For actual comp implementations, see for example
the [X-Pipe Extension API](https://github.com/xpipe-io/xpipe_java/tree/master/extension/src/main/java/io/xpipe/extension/comp)
.
