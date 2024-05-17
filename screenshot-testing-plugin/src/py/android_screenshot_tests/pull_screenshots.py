#!/usr/bin/env python3
# Copyright (c) Meta Platforms, Inc. and affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import getopt
import os
import sys


def usage(rest_args):
    print(
        "usage: ./scripts/screenshot_tests/pull_screenshots com.facebook.apk.name.tests [--generate-png]",
        file=sys.stderr,
    )
    print("got: %s" % rest_args)
    return


def pull_screenshots(
        source,
        temp_dir=None,
        record=None,
        verify=None,
        tolerance=None,
        failure_dir=None,
):
    if not os.path.exists(source):
        raise RuntimeError("source does not exists. path = %s" % source)

    record_dir = record
    verify_dir = verify
    tolerance = tolerance or 0.0

    if failure_dir:
        failure_dir = failure_dir
        if not os.path.exists(failure_dir):
            os.makedirs(failure_dir)

    if record or verify:
        # don't import this early, since we need PIL to import this
        from .recorder import Recorder

        recorder = Recorder(temp_dir, record_dir or verify_dir, failure_dir, tolerance)

        if verify:
            recorder.verify()
        else:
            recorder.record()


def main(argv):
    opt_list, rest_args = getopt.gnu_getopt(
        argv[1:],
        "eds:",
        [
            "source=",
            "record=",
            "verify=",
            "tolerance=",
            "failure-dir=",
            "temp-dir=",
        ],
    )

    if len(rest_args) != 0:
        usage(rest_args)
        return 2

    opts = dict(opt_list)

    tolerance = None
    try:
        tolerance = float(opts.get("--tolerance"))
    except (TypeError, ValueError):
        pass

    pull_screenshots(
        source=opts.get("--source"),
        temp_dir=opts.get("--temp-dir"),
        record=opts.get("--record"),
        verify=opts.get("--verify"),
        tolerance=tolerance,
        failure_dir=opts.get("--failure-dir"),
    )


if __name__ == "__main__":
    sys.exit(main(sys.argv))
