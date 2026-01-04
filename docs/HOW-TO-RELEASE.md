# How to release

- Pushed all changes
- Create a tag in the format `vX.Y.Z`
- Push the tag
- The `deploy.yml` workflow will run and publish the artifacts to Maven Central

## How to update the demo

1. Change to the `demo` branch in git

2. Run the updateDemo task:
   ```bash
   ./gradlew :sampleApp:updateDemo
   ```

   This will automatically:
   - Build the WASM distribution
   - Copy all files to the docs directory
   - Remove old WASM files that are no longer referenced

3. Commit and push the changes:
   ```bash
   git add docs/
   git commit -m "Update demo site"
   git push
   ```

4. The demo will be available at the GitHub Pages URL (typically
   `https://wavesonics.github.io/ComposeTextEditorLibrary/`)
