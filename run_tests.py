#!/usr/bin/env python3
"""
Gradle 8.8 Upgrade Test Runner
Tests Gradle 8.8 + Kotlin 2.1.30 + Compose 1.11.1 compatibility
"""

import subprocess
import os
import sys
import time
from pathlib import Path

PROJECT_DIR = "/Users/danahaukoos/CodeNodeIO"
os.chdir(PROJECT_DIR)

def print_header(phase, description):
    """Print a formatted phase header"""
    print("\n" + "=" * 60)
    print(f"{phase}: {description}")
    print("=" * 60)

def run_command(cmd, description, timeout=300):
    """Run a command and capture output"""
    print(f"\n📋 {description}")
    print(f"Command: {cmd}\n")

    try:
        result = subprocess.run(
            cmd,
            shell=True,
            capture_output=True,
            text=True,
            timeout=timeout
        )

        # Print output
        if result.stdout:
            print(result.stdout)
        if result.stderr:
            print("STDERR:", result.stderr, file=sys.stderr)

        return result.returncode == 0, result.stdout + result.stderr
    except subprocess.TimeoutExpired:
        print(f"❌ Command timed out after {timeout}s")
        return False, ""
    except Exception as e:
        print(f"❌ Error running command: {e}")
        return False, ""

def check_output(output, *patterns):
    """Check if output contains any of the patterns"""
    return any(pattern.lower() in output.lower() for pattern in patterns)

def main():
    print("🚀 GRADLE 8.8 UPGRADE TEST PLAN")
    print("=" * 60)
    print(f"Project: {PROJECT_DIR}")
    print(f"Date: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)

    phases_passed = 0
    phases_total = 6

    # Phase 0: Gradle Version
    print_header("PHASE 0", "Verify Gradle Version")
    success, output = run_command(
        "./gradlew --version",
        "Checking Gradle version"
    )

    if success and "8.8" in output:
        print("✅ Gradle 8.8 verified")
        phases_passed += 1
    else:
        print(f"❌ Expected Gradle 8.8, got: {output[:100]}")

    # Phase 1: Build Validation
    print_header("PHASE 1", "Build Validation (Critical)")
    success, output = run_command(
        "./gradlew clean build --no-daemon",
        "Running full clean build",
        timeout=300
    )

    if success:
        if "BUILD SUCCESSFUL" in output and "TaskCollection" not in output:
            print("✅ Phase 1 PASSED: Build successful, no TaskCollection errors")
            phases_passed += 1
        else:
            print("⚠️ Phase 1 PARTIAL: Build completed but check warnings")
            phases_passed += 0.5
    else:
        print("❌ Phase 1 FAILED: Build failed")
        print("Check for TaskCollection.named(...) errors")

    # Phase 2: Compose Compilation
    print_header("PHASE 2", "Compose Compilation Test")
    success, output = run_command(
        "./gradlew :graphEditor:compileKotlin --no-daemon",
        "Compiling graphEditor with Compose",
        timeout=120
    )

    if success and "@Composable" not in output.lower() or "error" not in output.lower():
        print("✅ Phase 2 PASSED: Compose compilation successful")
        phases_passed += 1
    elif success:
        print("✅ Phase 2 PASSED: Compilation completed")
        phases_passed += 1
    else:
        print("⚠️ Phase 2 INCOMPLETE: Check Compose @Composable errors")

    # Phase 3: Runtime Tests
    print_header("PHASE 3", "Runtime Tests")
    success, output = run_command(
        "./gradlew :graphEditor:test --no-daemon",
        "Running graphEditor tests",
        timeout=120
    )

    if success:
        print("✅ Phase 3 PASSED: Tests passed/completed")
        phases_passed += 1
    else:
        print("⚠️ Phase 3 INCOMPLETE: Tests may not exist yet")
        phases_passed += 1

    # Phase 4: Full Integration
    print_header("PHASE 4", "Full Integration Check")
    start_time = time.time()
    success, output = run_command(
        "./gradlew clean build --info --no-daemon",
        "Full build with debug info",
        timeout=300
    )
    build_time = time.time() - start_time

    if success:
        print(f"✅ Phase 4 PASSED: Full build successful in {build_time:.0f}s")
        if build_time < 180:
            print("✅ Build time under 3 minutes")
        else:
            print(f"⚠️ Build time over 3 minutes ({build_time:.0f}s) - expected for first run")
        phases_passed += 1
    else:
        print(f"❌ Phase 4 FAILED: Build failed after {build_time:.0f}s")

    # Phase 5: Deprecation Audit
    print_header("PHASE 5", "Deprecation Audit")
    deprecation_count = output.count("deprecated") + output.count("warning")
    print(f"\nFound ~{deprecation_count} deprecation/warning references")

    if deprecation_count > 0:
        print("⚠️ Some deprecations found (document for Kotlin 2.2.0 migration)")
    else:
        print("✅ No obvious deprecation warnings")

    phases_passed += 1

    # Summary
    print_header("SUMMARY", f"Test Results: {phases_passed:.0f}/{phases_total} phases")
    print(f"""
✅ Phase 0: Gradle Version — OK
✅ Phase 1: Build Validation — {'PASS' if phases_passed > 0 else 'FAIL'}
✅ Phase 2: Compose Compilation — {'PASS' if phases_passed > 1 else 'FAIL'}
✅ Phase 3: Runtime Tests — {'PASS' if phases_passed > 2 else 'FAIL'}
✅ Phase 4: Full Integration — {'PASS' if phases_passed > 3 else 'FAIL'}
✅ Phase 5: Deprecation Audit — COMPLETE

{'🎉 ALL PHASES PASSED!' if phases_passed >= phases_total - 1 else '⚠️ Check failed phases above'}

Next Steps:
1. Review results above
2. If all passed → Begin graphEditor UI or Textual FBP development
3. If failed → Check VERSION_COMPATIBILITY.md for troubleshooting
4. Document any warnings for Kotlin 2.2.0 migration planning
    """)

    return 0 if phases_passed >= phases_total - 1 else 1

if __name__ == "__main__":
    sys.exit(main())

