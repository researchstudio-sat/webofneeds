# HowTo Maven Release

1. Download and install GnuPG [here](https://gnupg.org/download/)
2. Import keys
    1. Save key from KeePass to `[HomeDir]\.gnupg`
    2. Import keys: `gpg --import RSA SAT.asc` and see if they are imported with `gpg --list-keys` & `gpg --list-secret-keys`
    3. Keys should be imported and shown
    4. Verify that key is now trusted with [ultimate] instead of [unknown]

        ```
        gpg --edit-key {KEY} trust quit
        # enter 5<RETURN> (I trust ultimately)
        # enter y<RETURN> (Really set this key to ultimate trust - Yes)
        ```

3. Add following code to your maven settings.xml.
    You will find it in your .m2 repository.

    ```
    <profiles>
            <profile>
                <id>sonatype-oss-release</id>
                <properties>
                    <gpg.keyname>[Keyname]</gpg.keyname>
                    <gpg.passphrase>**********</gpg.passphrase>
                    <gpg.defaultKeyring>false</gpg.defaultKeyring>
                    <gpg.useagent>true</gpg.useagent>
                    <gpg.lockMode>never</gpg.lockMode>
                    <gpg.homedir>[HomeDir]\.gnupg</gpg.homedir>
                </properties>
            </profile>
        </profiles>
    </profiles>
     <servers>
        <server>
            <id>ossrh</id>
            <username>rsa-sat</username>
            <password>**********</password>
        </server>
        <server>
            <id>TomcatServer</id>
            <username>admin</username>
            <password>admin</password>
        </server>
    </servers>
    ```

4. Run Maven release: `mvn release:clean release:prepare release:perform`
5. The process will ask for a password -> see KeePass
6. Push tags with: `git push --tags`
7. And push changes with: `git push`
8. Maven Release should be done!

For further information see [this instruction](https://dzone.com/articles/deploy-maven-central)
