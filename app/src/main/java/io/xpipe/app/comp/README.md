# Compound Components

As a basis, JavaFX nodes are created and manage via comps (compound components). 

## Principles

#### A comp is a Node/Region factory, not just another fancy wrapper for existing classes

It is advantageous to define a certain component to be a factory
that can create an instances of a JavaFX Node each time it is called.
By using this factory architecture, the scene contents can
be rebuilt entirely by invoking the root component factory.

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

