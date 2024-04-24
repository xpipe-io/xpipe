# Translations

This directory contains the translations for the strings and texts of the XPipe application. The original translations were created in english and initially translated automatically using [DeepL](https://www.deepl.com/en/translator). If you are interested in contributing to the translations, the main task is checking the automatically generated translations, which are pretty accurate most of the time. So you don't have to write long paragraphs, just perform occasional corrections.

## How-to

First of all, make sure that the language you want to contribute translations for is already set up here. If you don't find translations for your language in here, please create an issue in this repository and a maintainer will generate the initial automatic translations. This has to be done by a maintainer first as it requires a DeepL API subscription and some scripting.

If your language exists in the translations, you can simply submit corrections via a pull request if you find strings that are wrong.

### Correcting strings

The strings in XPipe are located in one of the `.properties` files in the `strings` directories. The set of strings is constantly expanded and some existing strings are refined. Therefore, the translations are frequently regenerated/retranslated. If you want to correct a string, you have to mark it as custom to prevent it being overridden when the translations are updated. So a string in a translation like
```
key=Wrong translation
```
has to be transformed into
```
#custom
key=Correct translation
```
to mark it being a custom string that should be kept. It is important to include the `#custom` annotation.

### Correcting texts

If you want to correct something in a text in a `.md` file, you don't have to do anything special as these are not being overwritten later on. Just perform and submit your changes.

### Improving automatic translations

If a string translated from english is wrong in many languages, it might also make sense to adjust the initial translation and context setting to improve the automatic translation. It is possible to augment the original english string and regenerate the translations for that by adding a `#context: ...` annotation to an original english string that is not translated correctly to improve the accuracy. You will already see some english strings that have this information added.

### Trying them out in action

If you have cloned this repository, you can automatically run a developer build of XPipe by following the instructions in the [contribution guide](/CONTRIBUTING.md). This will use your modified translations, so you can see how they look and behave in practice.

## Status

Here you can see the current status of the translations. Verified means that these were proof-read and corrected by an actual human at a certain version. Later versions might introduce new strings that are not yet proof-read, this is why version information is included.

| Language | Status         |
|----------|----------------|
| English  | Reference      |
| German   | Verified @ 9.0 |
| Danish   | Verified @ 9.0 |